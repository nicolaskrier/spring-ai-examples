package com.github.nicolaskrier.experimental.spring.ai.pope.search.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
class PopeSearchMcpServer {

    @Bean
    ToolCallbackProvider popeSearchToolCallbackProvider(PopeSearchTools popeSearchTools) {
        return MethodToolCallbackProvider.builder().toolObjects(popeSearchTools).build();
    }

    static void main(String[] args) {
        SpringApplication.run(PopeSearchMcpServer.class, args);
    }

}
