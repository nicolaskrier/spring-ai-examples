package com.github.nicolaskrier.experimental.spring.ai.time.mcp.server;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;

@SpringBootApplication
class TimeMcpServer {

    @Bean
    ToolCallback createCurrentDateTimeToolCallback() {
        return FunctionToolCallback.builder("currentDateTime", () -> ZonedDateTime.now(UTC).toString())
                .description("Get current date and time with UTC time zone in the ISO-8601 calendar system.")
                .build();
    }

    static void main(String[] args) {
        SpringApplication.run(TimeMcpServer.class, args);
    }

}
