package com.gorevplatformu.motorapi.guvenlik.dto;

import jakarta.validation.constraints.NotBlank;

public record KimlikBilgisiIstegi(@NotBlank String clientId, @NotBlank String clientSecret) {
}
