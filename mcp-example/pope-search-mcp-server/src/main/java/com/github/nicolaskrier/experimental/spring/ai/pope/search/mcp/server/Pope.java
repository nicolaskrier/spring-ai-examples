package com.github.nicolaskrier.experimental.spring.ai.pope.search.mcp.server;

import java.time.LocalDate;
import java.util.List;

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
