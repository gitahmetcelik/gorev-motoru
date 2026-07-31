package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * zamanlanmis_gorevler tablosundan vadesi gelenleri okuyup GorevGonderici uzerinden gorev uretir.
 * ShedLock, coklu worker instance'ta ayni anda sadece birinin calismasini garantiler.
 */
@Component
public class ZamanlanmisGorevCalistirici {

    private static final Logger log = LoggerFactory.getLogger(ZamanlanmisGorevCalistirici.class);

    private final ZamanlanmisGorevRepository zamanlanmisGorevRepository;
    private final GorevGonderici gorevGonderici;
    private final ObjectMapper objectMapper;

    public ZamanlanmisGorevCalistirici(ZamanlanmisGorevRepository zamanlanmisGorevRepository,
                                        GorevGonderici gorevGonderici, ObjectMapper objectMapper) {
        this.zamanlanmisGorevRepository = zamanlanmisGorevRepository;
        this.gorevGonderici = gorevGonderici;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 10_000)
    @SchedulerLock(name = "zamanlanmisGorevleriCalistir", lockAtLeastFor = "PT5S", lockAtMostFor = "PT2M")
    @Transactional
    public void calistir() {
        Instant simdi = Instant.now();
        List<ZamanlanmisGorev> zamaniGelenler =
                zamanlanmisGorevRepository.findByAktifTrueAndSonrakiCalismaLessThanEqual(simdi);
        for (ZamanlanmisGorev zamanlanmis : zamaniGelenler) {
            tetikle(zamanlanmis);
        }
    }

    private void tetikle(ZamanlanmisGorev zamanlanmis) {
        Instant hedeflenenZaman = zamanlanmis.getSonrakiCalisma();
        // Idempotency anahtari hedeflenen calisma zamanindan turetiliyor (simdiki zamandan degil):
        // ShedLock zaten coklu-instance yarisini engelliyor, ama bu ikinci bir guvence katmani —
        // ayni slot herhangi bir sebeple iki kez tetiklenmeye calisilirsa (orn. bu metod commit
        // olmadan yeniden calisirsa), mevcut idempotency-dedup (Faz3) ikinci Gorev'i engeller.
        String idempotencyAnahtari = "zamanlanmis:" + zamanlanmis.getId() + ":" + hedeflenenZaman.toEpochMilli();
        try {
            JsonNode payloadNode = objectMapper.readTree(zamanlanmis.getPayload());
            UUID gorevId = gorevGonderici.gonder(zamanlanmis.getTip(), payloadNode,
                    new GorevOpsiyonlari(idempotencyAnahtari, null, null));
            log.info("Zamanlanmis gorev tetiklendi: zamanlanmisId={}, hedeflenenZaman={}, uretilenGorevId={}",
                    zamanlanmis.getId(), hedeflenenZaman, gorevId);
        } catch (Exception e) {
            log.error("Zamanlanmis gorev tetiklenirken hata olustu, bu slot atlaniyor: {}", zamanlanmis.getId(), e);
        }
        Instant sonrakiCalisma =
                ZamanlanmisGorevZamanHesaplayici.sonrakiniHesapla(zamanlanmis.getCronIfadesi(), hedeflenenZaman);
        zamanlanmis.calistirildiOlarakIsaretle(hedeflenenZaman, sonrakiCalisma);
        zamanlanmisGorevRepository.save(zamanlanmis);
    }
}
