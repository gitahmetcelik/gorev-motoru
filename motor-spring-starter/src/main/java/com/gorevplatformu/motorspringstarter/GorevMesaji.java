package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record GorevMesaji(UUID gorevId, String tip, JsonNode payload, String traceId) {
}
