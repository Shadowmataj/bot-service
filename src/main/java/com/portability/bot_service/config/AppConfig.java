package com.portability.bot_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.portability.bot_service.service.PostgresChatMemory;

@Configuration
public class AppConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .build();
    }

    @Bean
    ChatMemory chatMemory(PostgresChatMemory postgresChatMemory) {
        return postgresChatMemory;
    }

}
