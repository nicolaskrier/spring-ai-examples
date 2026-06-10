package com.github.nicolaskrier.experimental.spring.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@SpringBootApplication
class ToolsExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsExample.class);

    private static final String SEARCHED_POPE_KEY = "searched_pope";

    @Value("classpath:/prompts/system-prompt.txt")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/templated-user-prompt.txt")
    private Resource templatedUserPromptResource;

    @Value("classpath:/prompts/user-prompt.txt")
    private Resource userPromptResource;

    @Value("${searched-pope:actual}")
    private String searchedPope;

    @Value("${previous-searched-popes-number:0}")
    private int previousSearchedPopesNumber;

    @Bean
    ToolCallback currentDateTimeToolCallback() {
        return FunctionToolCallback.builder("currentDateTime", () -> Instant.now().toString())
                .description("Get current date and time with UTC time zone in the ISO-8601 calendar system.")
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, PopeSearchTools popeSearchTools, ToolCallback currentDateTimeToolCallback) {
        return chatClientBuilder.defaultSystem(systemPromptResource)
                .defaultAdvisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID()))
                .defaultTools(currentDateTimeToolCallback, popeSearchTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    ApplicationRunner applicationRunner(ChatClient chatClient) {
        return _ -> {
            searchPope(chatClient);
            searchPreviousPopes(chatClient);
        };
    }

    private void searchPope(ChatClient chatClient) {
        var prompt = PromptTemplate.builder()
                .resource(templatedUserPromptResource)
                .variables(Map.of(SEARCHED_POPE_KEY, searchedPope))
                .build()
                .create();
        var pope = searchPope(prompt, chatClient);
        LOGGER.info("The {} pope is: {}", searchedPope, pope);
    }

    private void searchPreviousPopes(ChatClient chatClient) {
        IntStream.range(0, previousSearchedPopesNumber).forEach(_ -> {
            var prompt = createUserPrompt();
            var pope = searchPope(prompt, chatClient);
            LOGGER.info("The previous pope is: {}", pope);
        });
    }

    private static Pope searchPope(Prompt prompt, ChatClient chatClient) {
        return chatClient.prompt(prompt)
                .call()
                .entity(Pope.class, entityParamSpec -> entityParamSpec.useProviderStructuredOutput().validateSchema());
    }

    private Prompt createUserPrompt() {
        try {
            return new Prompt(userPromptResource.getContentAsString(Charset.defaultCharset()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read user prompt!", exception);
        }
    }

    static void main(String[] args) {
        SpringApplication.run(ToolsExample.class, args);
    }

}
