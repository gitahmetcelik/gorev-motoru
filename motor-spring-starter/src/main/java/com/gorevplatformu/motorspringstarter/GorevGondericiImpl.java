package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GorevGondericiImpl implements GorevGonderici {

    private final GorevRepository gorevRepository;
    private final GorevKayitIslemi gorevKayitIslemi;
    private final ObjectMapper objectMapper;

    public GorevGondericiImpl(GorevRepository gorevRepository, GorevKayitIslemi gorevKayitIslemi,
                               ObjectMapper objectMapper) {
        this.gorevRepository = gorevRepository;
        this.gorevKayitIslemi = gorevKayitIslemi;
        this.objectMapper = objectMapper;
    }

    @Override
    public UUID gonder(String tip, Object payload, GorevOpsiyonlari opsiyonlar) {
        var mevcut = gorevRepository.findByIdempotencyAnahtari(opsiyonlar.idempotencyAnahtari());
        if (mevcut.isPresent()) {
            return mevcut.get().getId();
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload JSON'a serilestirilemedi: " + tip, e);
        }

        try {
            return gorevKayitIslemi.olusturVeKuyrukla(tip, payloadJson, opsiyonlar);
        } catch (DataIntegrityViolationException e) {
            // Yarisma durumu: ayni idempotency anahtariyla es zamanli ikinci gonderim UNIQUE constraint'e carpti.
            return gorevRepository.findByIdempotencyAnahtari(opsiyonlar.idempotencyAnahtari())
                    .map(Gorev::getId)
                    .orElseThrow(() -> e);
        }
    }
}
