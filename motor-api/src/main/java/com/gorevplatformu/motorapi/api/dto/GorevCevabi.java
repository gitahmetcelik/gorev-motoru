package com.gorevplatformu.motorapi.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorspringstarter.Gorev;

import java.time.Instant;
import java.util.UUID;

public record GorevCevabi(UUID id, String tip, GorevDurumu durum, Integer oncelik, Instant planlananZaman,
                           Integer denemeSayisi, Integer ilerlemeYuzde, JsonNode sonuc, String hata,
                           boolean iptalIstendi, String traceId, Instant olusturulma, Instant guncellenme) {

    public static GorevCevabi olustur(Gorev gorev, ObjectMapper objectMapper) {
        JsonNode sonuc = okuJson(gorev.getSonuc(), objectMapper);
        return new GorevCevabi(gorev.getId(), gorev.getTip(), gorev.getDurum(), gorev.getOncelik(),
                gorev.getPlanlananZaman(), gorev.getDenemeSayisi(), gorev.getIlerlemeYuzde(), sonuc,
                gorev.getHata(), gorev.isIptalIstendiMi(), gorev.getTraceId(), gorev.getOlusturulma(),
                gorev.getGuncellenme());
    }

    private static JsonNode okuJson(String json, ObjectMapper objectMapper) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Gorev JSON alani parse edilemedi", e);
        }
    }
}
