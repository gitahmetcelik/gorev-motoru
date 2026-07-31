package com.gorevplatformu.motorapi.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GorevOlusturmaIstegi(
        @NotBlank String tip,
        @NotNull JsonNode payload,
        String idempotencyAnahtari,
        Integer oncelik,
        Instant planlananZaman) {
}
