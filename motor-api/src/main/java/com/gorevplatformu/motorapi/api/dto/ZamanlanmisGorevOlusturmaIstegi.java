package com.gorevplatformu.motorapi.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZamanlanmisGorevOlusturmaIstegi(
        @NotBlank String tip,
        @NotBlank
        @Schema(description = "Spring 6-alanli cron formati: saniye dakika saat gun ay haftaGunu",
                example = "0 0 3 * * *")
        String cronIfadesi,
        @NotNull JsonNode payload,
        Boolean aktif) {
}
