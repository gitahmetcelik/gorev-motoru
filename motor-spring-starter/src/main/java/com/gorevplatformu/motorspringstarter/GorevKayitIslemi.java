package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
class GorevKayitIslemi {

    private final GorevRepository gorevRepository;
    private final GidenMesajRepository gidenMesajRepository;
    private final TraceBaglamServisi traceBaglamServisi;

    GorevKayitIslemi(GorevRepository gorevRepository, GidenMesajRepository gidenMesajRepository,
                     TraceBaglamServisi traceBaglamServisi) {
        this.gorevRepository = gorevRepository;
        this.gidenMesajRepository = gidenMesajRepository;
        this.traceBaglamServisi = traceBaglamServisi;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    UUID olusturVeKuyrukla(String tip, String payloadJson, GorevOpsiyonlari opsiyonlar) {
        Gorev gorev = new Gorev(
                tip,
                payloadJson,
                opsiyonlar.idempotencyAnahtari(),
                opsiyonlar.oncelik(),
                opsiyonlar.planlananZaman(),
                traceBaglamServisi.mevcutYadaYeniTraceId()
        );
        gorevRepository.save(gorev);
        int gecikmeMs = gecikmeHesapla(opsiyonlar.planlananZaman());
        gidenMesajRepository.save(new GidenMesaj(gorev.getId(), gecikmeMs));
        return gorev.getId();
    }

    // gecikme_ms INT (int32) oldugu icin ~24.8 gunden uzun planlananZaman gecikmeleri overflow
    // eder - bu araligin kapsami zaten zamanlanmis_gorevler/cron mekanizmasinin isi, burada
    // ayrica ele alinmiyor.
    private int gecikmeHesapla(Instant planlananZaman) {
        if (planlananZaman == null) {
            return 0;
        }
        long ms = Duration.between(Instant.now(), planlananZaman).toMillis();
        return (int) Math.max(ms, 0);
    }
}
