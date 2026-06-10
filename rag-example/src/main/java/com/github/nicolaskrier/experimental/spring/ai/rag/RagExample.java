package com.github.nicolaskrier.experimental.spring.ai.rag;

import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootApplication
class RagExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagExample.class);

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
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        return chatClientBuilder.defaultSystem(systemPromptResource)
                .defaultAdvisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID()))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(createSearchRequest(searchedPopePontiffNumber))
                                .build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @Bean
    @Order(2)
    ApplicationRunner dataRetrieverApplicationRunner(ChatClient chatClient) {
        return _ -> {
            searchPope(chatClient);
            searchNextPopes(chatClient);
        };
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

    private void searchPope(ChatClient chatClient) {
        var prompt = PromptTemplate.builder()
                .resource(templatedUserPromptResource)
                .variables(Map.of(SEARCHED_POPE_PONTIFF_NUMBER_KEY, searchedPopePontiffNumber))
                .build()
                .create();
        var pope = searchPope(prompt, chatClient);
        LOGGER.info("The {} pope is: {}", searchedPopePontiffNumber, pope);
    }

    private void searchNextPopes(ChatClient chatClient) {
        IntStream.range(0, nextSearchedPopesNumber).forEach(_ -> {
            var prompt = createUserPrompt();
            var pope = searchPope(prompt, chatClient);
            LOGGER.info("The next pope is: {}", pope);
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
        SpringApplication.run(RagExample.class, args);
    }

}
