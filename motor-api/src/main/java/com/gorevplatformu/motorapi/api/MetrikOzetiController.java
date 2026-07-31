package com.gorevplatformu.motorapi.api;

import com.gorevplatformu.motorapi.api.dto.MetrikOzetiCevabi;
import com.gorevplatformu.motorspringstarter.CalisanGorevKayitDefteri;
import com.gorevplatformu.motorspringstarter.GorevMetrikleri;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard'un canli kuyruk metrikleri paneli icin kucuk bir JSON ozet — Prometheus text
 * formatini istemci tarafinda parse etmek yerine.
 */
@RestController
@RequestMapping("/metrikler")
public class MetrikOzetiController {

    private final GorevMetrikleri gorevMetrikleri;
    private final CalisanGorevKayitDefteri calisanGorevKayitDefteri;

    public MetrikOzetiController(GorevMetrikleri gorevMetrikleri,
                                  CalisanGorevKayitDefteri calisanGorevKayitDefteri) {
        this.gorevMetrikleri = gorevMetrikleri;
        this.calisanGorevKayitDefteri = calisanGorevKayitDefteri;
    }

    @GetMapping("/ozet")
    public MetrikOzetiCevabi ozet() {
        Map<String, Long> derinlikler = gorevMetrikleri.kuyrukDerinlikleri();
        return new MetrikOzetiCevabi(derinlikler.get("yuksek"), derinlikler.get("normal"),
                derinlikler.get("dusuk"), derinlikler.get("dlq"),
                calisanGorevKayitDefteri.calisanGorevIdleri().size());
    }
}
