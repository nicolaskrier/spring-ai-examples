package com.github.nicolaskrier.experimental.spring.ai.mcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@SpringBootApplication
class McpClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpClientExample.class);

    private static final String SEARCHED_POPE_KEY = "searched_pope";

    private static final String FORMAT_KEY = "format";

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
    BeanOutputConverter<Pope> beanOutputConverter() {
        return new BeanOutputConverter<>(Pope.class);
    }

    @Bean
    ApplicationRunner applicationRunner(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, BeanOutputConverter<Pope> beanOutputConverter, ToolCallbackProvider toolCallbackProvider) {
        return _ -> {
            var chatClient = createChatClient(chatClientBuilder, chatMemory, toolCallbackProvider);
            searchPope(chatClient, beanOutputConverter);
            searchPreviousPopes(chatClient, beanOutputConverter);
        };
    }

    private ChatClient createChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider toolCallbackProvider) {
        return chatClientBuilder.defaultSystem(systemPromptResource)
                .defaultAdvisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID().toString()))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    private void searchPope(ChatClient chatClient, BeanOutputConverter<Pope> beanOutputConverter) {
        var promptVariables = Map.<String, Object>of(
                SEARCHED_POPE_KEY, searchedPope,
                FORMAT_KEY, beanOutputConverter.getFormat()
        );
        var prompt = PromptTemplate.builder()
                .resource(templatedUserPromptResource)
                .variables(promptVariables)
                .build()
                .create();
        var pope = searchPope(prompt, chatClient, beanOutputConverter);
        LOGGER.info("The {} pope is: {}", promptVariables.get(SEARCHED_POPE_KEY), pope);
    }

    private void searchPreviousPopes(ChatClient chatClient, BeanOutputConverter<Pope> beanOutputConverter) {
        IntStream.range(0, previousSearchedPopesNumber).forEach(_ -> {
            var prompt = createUserPrompt();
            var pope = searchPope(prompt, chatClient, beanOutputConverter);
            LOGGER.info("The previous pope is: {}", pope);
        });
    }

    private static Pope searchPope(Prompt prompt, ChatClient chatClient, BeanOutputConverter<Pope> beanOutputConverter) {
        var generatedTextOutput = chatClient.prompt(prompt)
                .call()
                .content();

        return beanOutputConverter.convert(Objects.requireNonNull(generatedTextOutput));
    }

    private Prompt createUserPrompt() {
        try {
            return new Prompt(userPromptResource.getContentAsString(Charset.defaultCharset()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read user prompt!", exception);
        }
    }

    static void main(String[] args) {
        SpringApplication.run(McpClientExample.class, args);
    }

}
