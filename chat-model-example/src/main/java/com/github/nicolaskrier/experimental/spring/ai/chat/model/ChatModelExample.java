package com.github.nicolaskrier.experimental.spring.ai.chat.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
class ChatModelExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatModelExample.class);

    @Bean
    ApplicationRunner applicationRunner(ChatModel chatModel) {
        return _ -> {
            var systemMessage = new SystemMessage("You are a helpful assistant that helps people find information. You don't provide any explanations, just the answers. For simple answers, no punctuation is needed.");
            var userMessage = new UserMessage("Who is the actual pope? Roman numbers could be used if necessary.");
            var generatedTextOutput = chatModel.call(systemMessage, userMessage);
            LOGGER.info("The actual pope is '{}'.", generatedTextOutput);
        };
    }

    static void main() {
        SpringApplication.run(ChatModelExample.class);
    }

}
