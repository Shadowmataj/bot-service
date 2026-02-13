package com.portability.bot_service.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for buffering WhatsApp messages and processing them after a timeout
 * period.
 * Messages from the same phone number are accumulated and processed together
 * if no new message arrives within 8 seconds.
 */
@Service
public class WhatsAppMessageBufferService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMessageBufferService.class);
    private static final long TIMEOUT_SECONDS = 8;

    @Autowired
    private ChatService chatService;

    @Autowired
    private OkHttpClient client;

    @Value("${whatsapp.api-url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.access-identifier}")
    private String whatsappAccessIdentifier;

    private final Map<String, MessageBuffer> bufferMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * Add a message to the buffer for the given phone number.
     * Resets the timeout each time a new message arrives.
     * 
     * @param message     The message text
     * @param phoneNumber The user's phone number
     */
    public void addMessage(String message, String phoneNumber, String phoneNumberId) {
        logger.info("Adding message to buffer for {}: {}", phoneNumber, message);

        bufferMap.compute(phoneNumber, (key, existingBuffer) -> {
            if (existingBuffer == null) {
                existingBuffer = new MessageBuffer(phoneNumberId);
            }

            // Cancel the previous timeout if it exists
            if (existingBuffer.scheduledTask != null && !existingBuffer.scheduledTask.isDone()) {
                existingBuffer.scheduledTask.cancel(false);
                logger.debug("Cancelled previous timeout for {}", phoneNumber);
            }

            // Add the new message
            existingBuffer.messages.add(message);

            // Schedule a new timeout
            existingBuffer.scheduledTask = scheduler.schedule(
                    () -> processBuffer(phoneNumber),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);

            logger.debug("Scheduled processing for {} in {} seconds. Buffer size: {}",
                    phoneNumber, TIMEOUT_SECONDS, existingBuffer.messages.size());

            return existingBuffer;
        });
    }

    /**
     * Process all buffered messages for a phone number.
     * Concatenates messages and sends them to the chat service.
     * 
     * @param phoneNumber The phone number to process
     */
    private void processBuffer(String phoneNumber) {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageBuffer buffer = bufferMap.remove(phoneNumber);

        if (buffer == null || buffer.messages.isEmpty()) {
            logger.warn("No messages to process for {}", phoneNumber);
            return;
        }

        try {
            // Concatenate all messages with line breaks
            String concatenatedMessage = String.join("\n", buffer.messages);
            String phoneNumberId = buffer.phoneNumberId;

            logger.info(
                    "Processing buffered messages for {}. Message count: {}. Combined message: {}. PhoneNumberId: {}",
                    phoneNumber, buffer.messages.size(), concatenatedMessage, phoneNumberId);

            // Process the combined message
            String response = chatService.getBotResponse(concatenatedMessage, phoneNumber);

            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsString(Map.of(
                            "messaging_product", "whatsapp",
                            "to", phoneNumber,
                            "type", "text",
                            "text", Map.of("body", response))),
                    okhttp3.MediaType.parse("application/json"));

            Request whatsAppRequest = new Request.Builder()
                    .url(whatsappApiUrl + phoneNumberId + "/messages")
                    .post(requestBody)
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", "Bearer " + whatsappAccessIdentifier)
                    .build();

            Response whatsappResponse = client.newCall(whatsAppRequest).execute();

            if (!whatsappResponse.isSuccessful()) {
                logger.error("Failed to send WhatsApp message to {}: {} - {}",
                        phoneNumber, whatsappResponse.code(), whatsappResponse.body().string());
            } else {
                logger.info("Successfully sent WhatsApp message");
            }

        } catch (IOException e) {
            logger.error("Error processing buffered messages for {}: {}", phoneNumber, e.getMessage(), e);
        }
    }

    /**
     * Get the current buffer size for a phone number.
     * Useful for monitoring and debugging.
     * 
     * @param phoneNumber The phone number
     * @return The number of buffered messages, or 0 if no buffer exists
     */
    public int getBufferSize(String phoneNumber) {
        MessageBuffer buffer = bufferMap.get(phoneNumber);
        return buffer != null ? buffer.messages.size() : 0;
    }

    /**
     * Cleanup resources when the service is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down WhatsAppMessageBufferService");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Internal class to hold buffered messages and scheduled task for each phone
     * number.
     */
    private static class MessageBuffer {
        final List<String> messages;
        ScheduledFuture<?> scheduledTask;
        String phoneNumberId;

        MessageBuffer(String phoneNumberId) {
            this.phoneNumberId = phoneNumberId;
            this.messages = new ArrayList<>();
        }
    }
}
