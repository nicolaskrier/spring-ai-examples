package com.github.nicolaskrier.experimental.spring.ai.rag;

import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootApplication
class RagExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagExample.class);

    private static final String FORMAT_KEY = "format";

    private static final String PONTIFF_NUMBER_KEY = "pontiffNumber";

    private static final String SEARCHED_POPE_PONTIFF_NUMBER_KEY = "searched_pope_pontiff_number";

    @Value("classpath:/prompts/system-prompt.txt")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/templated-user-prompt.txt")
    private Resource templatedUserPromptResource;

    @Value("classpath:/prompts/user-prompt.txt")
    private Resource userPromptResource;

    @Value("classpath:/data/popes.json")
    private Resource popesDataResource;

    @Value("${searched-pope-pontiff-number:267}")
    private int searchedPopePontiffNumber;

    @Value("${next-searched-popes-number:0}")
    private int nextSearchedPopesNumber;

    @Bean
    DocumentReader documentReader() {
        JsonMetadataGenerator jsonMetadataGenerator = jsonMap -> {
            if (jsonMap.containsKey(PONTIFF_NUMBER_KEY)) {
                return Map.of(PONTIFF_NUMBER_KEY, jsonMap.get(PONTIFF_NUMBER_KEY));
            } else {
                return Map.of();
            }
        };

        return new JsonReader(popesDataResource, jsonMetadataGenerator);
    }

    @Bean
    @Order(1)
    ApplicationRunner dataLoaderApplicationRunner(DocumentReader documentReader, QdrantVectorStore qdrantVectorStore, QdrantVectorStoreProperties qdrantVectorStoreProperties) {
        return _ -> {
            @SuppressWarnings("resource")
            var qdrantClient = qdrantVectorStore.<QdrantClient>getNativeClient().orElseThrow();
            var storedPopesCount = qdrantClient.countAsync(qdrantVectorStoreProperties.getCollectionName()).get(1, SECONDS);

            if (storedPopesCount == 0L) {
                LOGGER.info("Loading documents corresponding to popes into vector store.");
                var documents = documentReader.read();
                qdrantVectorStore.add(documents);
                LOGGER.info("{} documents corresponding to popes loaded into vector store.", documents.size());
            } else {
                LOGGER.info("Documents corresponding to popes have already been loaded into vector store.");
            }
        };
    }

    @Bean
    BeanOutputConverter<Pope> beanOutputConverter() {
        return new BeanOutputConverter<>(Pope.class);
    }

    @Bean
    @Order(2)
    ApplicationRunner dataRetrieverApplicationRunner(
            ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            BeanOutputConverter<Pope> beanOutputConverter
    ) {
        return _ -> {
            var chatClient = createChatClient(chatClientBuilder, chatMemory, vectorStore);
            searchPope(chatClient, beanOutputConverter);
            searchNextPopes(chatClient, beanOutputConverter);
        };
    }

    private ChatClient createChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        return chatClientBuilder.defaultSystem(systemPromptResource)
                .defaultAdvisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID().toString()))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(createSearchRequest(searchedPopePontiffNumber))
                                .build(),
                        new SystemFirstSortingAdvisor(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    private static SearchRequest createSearchRequest(int searchedPopePontiffNumber) {
        var filterExpression = new Filter.Expression(
                Filter.ExpressionType.GTE,
                new Filter.Key(PONTIFF_NUMBER_KEY),
                new Filter.Value(searchedPopePontiffNumber)
        );

        return SearchRequest.builder()
                .filterExpression(filterExpression)
                .build();
    }

    private void searchPope(ChatClient chatClient, BeanOutputConverter<Pope> beanOutputConverter) {
        var promptVariables = Map.<String, Object>of(
                SEARCHED_POPE_PONTIFF_NUMBER_KEY, searchedPopePontiffNumber,
                FORMAT_KEY, beanOutputConverter.getFormat()
        );
        var prompt = PromptTemplate.builder()
                .resource(templatedUserPromptResource)
                .variables(promptVariables)
                .build()
                .create();
        var pope = searchPope(prompt, chatClient, beanOutputConverter);
        LOGGER.info("The {} pope is: {}", promptVariables.get(SEARCHED_POPE_PONTIFF_NUMBER_KEY), pope);
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

    // TODO: Remove this temporary solution once this issue is fixed:
    //  https://github.com/spring-projects/spring-ai/issues/4170
    static class SystemFirstSortingAdvisor implements BaseAdvisor {

        @Override
        public @NonNull ChatClientRequest before(@NonNull ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
            var messages = chatClientRequest.prompt().getInstructions();
            messages.sort(Comparator.comparing(message -> message.getMessageType() == MessageType.SYSTEM ? 0 : 1));
            var prompt = chatClientRequest.prompt()
                    .mutate()
                    .messages(messages)
                    .build();

            return chatClientRequest.mutate()
                    .prompt(prompt)
                    .build();
        }

        @Override
        public @NonNull ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
            return chatClientResponse;
        }

        @Override
        public int getOrder() {
            return 0;
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
        SpringApplication.run(RagExample.class, args);
    }

}
