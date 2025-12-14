package com.github.nicolaskrier.experimental.spring.ai.mistral.ai.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

import static org.springframework.ai.mistralai.api.MistralAiApi.ChatCompletionMessage.Role.SYSTEM;
import static org.springframework.ai.mistralai.api.MistralAiApi.ChatCompletionMessage.Role.USER;

@SpringBootApplication
class MistralAiApiExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(MistralAiApiExample.class);

    @Bean
    MistralAiApi mistralAiApi() {
        var mistralAiApiKey = System.getenv("MISTRAL_AI_API_KEY");
        Assert.hasText(mistralAiApiKey, "Mistral AI API key must be set!");

        return MistralAiApi.builder().apiKey(mistralAiApiKey).build();
    }

    @Bean
    ApplicationRunner applicationRunner(MistralAiApi mistralAiApi) {
        return _ -> {
            var systemChatCompletionMessage = new MistralAiApi.ChatCompletionMessage(
                    "You are a helpful assistant that helps people find information. You don't provide any explanations, just the answers. For simple answers, no punctuation is needed.",
                    SYSTEM
            );
            var userChatCompletionMessage = new MistralAiApi.ChatCompletionMessage(
                    "Who is the actual pope? Roman numbers could be used if necessary.",
                    USER
            );
            var chatCompletionRequest = new MistralAiApi.ChatCompletionRequest(
                    List.of(systemChatCompletionMessage, userChatCompletionMessage),
                    "mistral-small-latest"
            );
            var chatCompletion = mistralAiApi.chatCompletionEntity(chatCompletionRequest).getBody();
            var choice = Objects.requireNonNull(chatCompletion).choices().getFirst();
            var chatCompletionMessageContent = choice.message().content();
            LOGGER.info("The actual pope is '{}'.", chatCompletionMessageContent);
        };
    }

    static void main() {
        SpringApplication.run(MistralAiApiExample.class);
    }

}
