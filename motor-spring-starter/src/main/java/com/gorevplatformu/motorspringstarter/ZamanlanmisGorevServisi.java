package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ZamanlanmisGorevServisi {

    private final ZamanlanmisGorevRepository zamanlanmisGorevRepository;
    private final GorevTipiKayitDefteri kayitDefteri;
    private final ObjectMapper objectMapper;

    public ZamanlanmisGorevServisi(ZamanlanmisGorevRepository zamanlanmisGorevRepository,
                                    GorevTipiKayitDefteri kayitDefteri, ObjectMapper objectMapper) {
        this.zamanlanmisGorevRepository = zamanlanmisGorevRepository;
        this.kayitDefteri = kayitDefteri;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ZamanlanmisGorev olustur(String tip, String cronIfadesi, JsonNode payload, Boolean aktif) {
        kayitDefteri.tanimBul(tip); // bilinmeyen tip -> IllegalArgumentException

        Instant sonrakiCalisma;
        try {
            sonrakiCalisma = ZamanlanmisGorevZamanHesaplayici.sonrakiniHesapla(cronIfadesi, Instant.now());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Gecersiz cron ifadesi (Spring 6-alanli format bekleniyor: saniye dakika saat gun ay "
                            + "haftaGunu): " + cronIfadesi, e);
        }
        if (sonrakiCalisma == null) {
            throw new IllegalArgumentException("Cron ifadesi hicbir gelecek calisma zamani uretmiyor: " + cronIfadesi);
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload JSON'a serilestirilemedi", e);
        }

        ZamanlanmisGorev zamanlanmis = new ZamanlanmisGorev(tip, cronIfadesi, payloadJson,
                aktif == null || aktif, sonrakiCalisma);
        return zamanlanmisGorevRepository.save(zamanlanmis);
    }
}
