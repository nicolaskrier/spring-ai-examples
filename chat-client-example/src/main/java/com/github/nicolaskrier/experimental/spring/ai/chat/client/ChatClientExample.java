package com.github.nicolaskrier.experimental.spring.ai.chat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@SpringBootApplication
class ChatClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatClientExample.class);

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

    @Value("${next-searched-popes-number:0}")
    private int nextSearchedPopesNumber;

    @Bean
    BeanOutputConverter<Pope> beanOutputConverter() {
        return new BeanOutputConverter<>(Pope.class);
    }

    @Bean
    ApplicationRunner applicationRunner(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, BeanOutputConverter<Pope> beanOutputConverter) {
        return _ -> {
            var chatClient = createChatClient(chatClientBuilder, chatMemory);
            searchPope(chatClient, beanOutputConverter);
            searchNextPopes(chatClient, beanOutputConverter);
        };
    }

    private ChatClient createChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        return chatClientBuilder.defaultSystem(systemPromptResource)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
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

    private void searchNextPopes(ChatClient chatClient, BeanOutputConverter<Pope> beanOutputConverter) {
        IntStream.range(0, nextSearchedPopesNumber).forEach(_ -> {
            var prompt = createUserPrompt();
            var pope = searchPope(prompt, chatClient, beanOutputConverter);
            LOGGER.info("The next pope is: {}", pope);
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

    record Pope(
            int pontiffNumber,
            LocalDate pontiffStartDate,
            LocalDate pontiffEndDate,
            LocalDate birthDate,
            LocalDate deathDate,
            String englishName,
            String latinName,
            String personalName,
            List<String> nationalities
    ) {
        Pope {
            nationalities = List.copyOf(nationalities);
        }
    }

    static void main(String[] args) {
        SpringApplication.run(ChatClientExample.class, args);
    }

}
