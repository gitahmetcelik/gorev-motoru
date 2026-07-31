package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OluMektupKutusuServisi {

    private final OluMektupKutusuRepository oluMektupKutusuRepository;
    private final GorevRepository gorevRepository;
    private final GidenMesajRepository gidenMesajRepository;

    public OluMektupKutusuServisi(OluMektupKutusuRepository oluMektupKutusuRepository,
                                   GorevRepository gorevRepository,
                                   GidenMesajRepository gidenMesajRepository) {
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
        this.gorevRepository = gorevRepository;
        this.gidenMesajRepository = gidenMesajRepository;
    }

    @Transactional
    public void yenidenGonder(UUID oluMektupKutusuId) {
        OluMektupKutusu kayit = oluMektupKutusuRepository.findById(oluMektupKutusuId)
                .orElseThrow(() -> new IllegalArgumentException("Olu mektup kutusu kaydi bulunamadi: " + oluMektupKutusuId));

        if (kayit.getGorevId() == null) {
            throw new IllegalStateException(
                    "Bu kayit gorevId'si bilinmeyen zehirli bir mesaj, yeniden gonderilemez: " + oluMektupKutusuId);
        }

        Gorev gorev = gorevRepository.findById(kayit.getGorevId())
                .orElseThrow(() -> new IllegalStateException("Gorev bulunamadi: " + kayit.getGorevId()));

        // deneme_sayisi kasten sifirlanmiyor: gorev_denemeleri'ndeki (gorev_id, deneme_no) UNIQUE kisiti
        // gorevin tum omru boyunca gecerli, sifirlama onceki denemelerle carpisan bir deneme_no uretirdi.
        // Yeniden gonderim, kalan retry butcesiyle (varsa) devam eder - "MailHog'u ac, DLQ'dan yeniden
        // gonder -> basarili" senaryosunda zaten tek bir basarili deneme yeterli.
        gorev.durumGecisYap(GorevDurumu.BEKLIYOR);
        gorevRepository.save(gorev);
        gidenMesajRepository.save(new GidenMesaj(gorev.getId()));

        kayit.yenidenGonderildiOlarakIsaretle();
        oluMektupKutusuRepository.save(kayit);
    }
}
