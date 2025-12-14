package com.github.nicolaskrier.experimental.spring.ai.ollama.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.springframework.ai.ollama.api.OllamaApi.Message.Role.SYSTEM;
import static org.springframework.ai.ollama.api.OllamaApi.Message.Role.USER;

@SpringBootApplication
class OllamaApiExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaApiExample.class);

    @Bean
    OllamaApi ollamaApi() {
        return OllamaApi.builder().build();
    }

    @Bean
    ApplicationRunner applicationRunner(OllamaApi ollamaApi) {
        return _ -> {
            var systemRequestMessage = OllamaApi.Message.builder(SYSTEM)
                    .content("You are a helpful assistant that helps people find information. You don't provide any explanations, just the answers. For simple answers, no punctuation is needed.")
                    .build();
            var userRequestMessage = OllamaApi.Message.builder(USER)
                    .content("Who is the actual pope? Roman numbers could be used if necessary.")
                    .build();
            var chatRequest = OllamaApi.ChatRequest.builder("mistral-small3.2:latest")
                    .messages(List.of(systemRequestMessage, userRequestMessage))
                    .build();
            var chatResponse = ollamaApi.chat(chatRequest);
            var responseMessage = chatResponse.message();
            var responseMessageContent = responseMessage.content();
            LOGGER.info("The actual pope is '{}'.", responseMessageContent);
        };
    }

    static void main() {
        SpringApplication.run(OllamaApiExample.class);
    }

}
