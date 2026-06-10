package com.github.nicolaskrier.experimental.spring.ai.chat.client;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

record Pope(
        int pontiffNumber,
        LocalDate pontiffStartDate,
        @Nullable LocalDate pontiffEndDate,
        LocalDate birthDate,
        @Nullable LocalDate deathDate,
        String englishName,
        String latinName,
        String personalName,
        List<String> nationalities
) {
    Pope {
        nationalities = List.copyOf(nationalities);
    }
}
