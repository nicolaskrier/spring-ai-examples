package com.github.nicolaskrier.experimental.spring.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
class PopeSearchTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopeSearchTools.class);

    private final List<Pope> popes = List.of(
            new Pope(
                    264,
                    LocalDate.of(1978, 10, 16),
                    LocalDate.of(2005, 4, 2),
                    LocalDate.of(1920, 5, 18),
                    LocalDate.of(2005, 4, 2),
                    "John Paul II",
                    "IOANNES PAULUS Secundus",
                    "Karol Józef Wojtyła",
                    List.of("Polish")
            ),
            new Pope(
                    265,
                    LocalDate.of(2005, 4, 19),
                    LocalDate.of(2013, 2, 28),
                    LocalDate.of(1927, 4, 16),
                    LocalDate.of(2022, 12, 31),
                    "Benedict XVI",
                    "BENEDICTVS Sextus Decimus",
                    "Joseph Alois Ratzinger",
                    List.of("German")
            ),
            new Pope(
                    266,
                    LocalDate.of(2013, 3, 13),
                    LocalDate.of(2025, 4, 21),
                    LocalDate.of(1936, 12, 17),
                    LocalDate.of(2025, 4, 21),
                    "Francis",
                    "FRANCISCVS",
                    "Jorge Mario Bergoglio",
                    List.of("Argentine")
            ),
            new Pope(
                    267,
                    LocalDate.of(2025, 5, 8),
                    null,
                    LocalDate.of(1955, 9, 14),
                    null,
                    "Leo XIV",
                    "LEO Quartus Decimus",
                    "Robert Francis Prevost",
                    List.of("American", "Peruvian")
            )
    );

    @Tool(description = "Search a pope by using his pontiff number.")
    Pope searchPopeByPontiffNumber(@ToolParam(description = "The pontiff number used to search the pope.") int pontiffNumber) {
        LOGGER.info("Search pope by the following pontiff number: '{}'.", pontiffNumber);

        return popes.stream()
                .filter(pope -> pope.pontiffNumber() == pontiffNumber)
                .findFirst()
                .orElseThrow();
    }

    @Tool(description = "Search a pope by using a date that must included in the pope pontiff period.")
    Pope searchPopeByDate(@ToolParam(description = "The date that must included in the pope pontiff period.") LocalDate date) {
        LOGGER.info("Search pope by the following date: '{}'.", date);

        return popes.stream()
                .filter(pope -> pope.pontiffStartDate().isEqual(date) || pope.pontiffStartDate().isBefore(date))
                .filter(pope -> pope.pontiffEndDate() == null || pope.pontiffEndDate().isEqual(date) || pope.pontiffEndDate().isAfter(date))
                .findFirst()
                .orElseThrow();
    }

}
