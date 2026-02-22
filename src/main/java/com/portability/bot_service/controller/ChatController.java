package com.portability.bot_service.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portability.bot_service.model.dto.WhatsAppResponse;
import com.portability.bot_service.service.ChatService;
import com.portability.bot_service.service.WhatsAppMessageBufferService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService service;

    @Value("${whatsapp.api-url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.access-identifier}")
    private String whatsappAccessIdentifier;

    @Autowired
    private WhatsAppMessageBufferService whatsAppBufferService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${whatsapp.verify-token}")
    private String whatsappVerifyToken;

    @GetMapping("ask")
    public ResponseEntity<String> askBot(
            @RequestParam String message,
            @RequestParam String phoneNumber) {
        try {
            String res = service.getBotResponse(message, phoneNumber);
            return ResponseEntity.ok(res);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test endpoint for WhatsApp integration. This can be used to verify that the
     * endpoint is working without triggering the message buffering logic.
     * 
     * @param message     The message text
     * @param phoneNumber The user's phone number
     * @return Acknowledgment response
     */
    @GetMapping("whatsapp")
    public ResponseEntity<String> testWhatsApp(
            @RequestParam("hub.mode") String hubMode,
            @RequestParam("hub.challenge") String hubChallenge,
            @RequestParam("hub.verify_token") String hubVerifyToken) {
        if (hubVerifyToken.equals(whatsappVerifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
        }
    }

    /**
     * WhatsApp integration endpoint with message buffering.
     * Messages are accumulated and processed after 8 seconds of inactivity.
     * 
     * @param message     The message text
     * @param phoneNumber The user's phone number
     * @return Acknowledgment response
     */
    @PostMapping("whatsapp")
    public ResponseEntity<WhatsAppResponse> receiveWhatsAppMessage(HttpServletRequest req) {
        try {
            JsonNode root = objectMapper.readTree(req.getInputStream());
            JsonNode value = root.path("entry").get(0).path("changes").get(0).path("value");
            String phoneNumberId = value.path("metadata").path("phone_number_id").asText();
            JsonNode messageNode = value.path("messages").get(0);
            String type = messageNode.path("type").asText();
            String message = messageNode.path("text").path("body").asText();
            String rawNumber = messageNode.path("from").asText();
            String phoneNumber = rawNumber.substring(0, 2) + rawNumber.substring(3);

            whatsAppBufferService.addMessage(message, phoneNumber, phoneNumberId);
            int bufferSize = whatsAppBufferService.getBufferSize(phoneNumber);

            WhatsAppResponse response = new WhatsAppResponse(
                    "Message received",
                    "Your message has been queued and will be processed shortly",
                    bufferSize);

            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            WhatsAppResponse errorResponse = new WhatsAppResponse(
                    "Error",
                    "Failed to process message",
                    0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
