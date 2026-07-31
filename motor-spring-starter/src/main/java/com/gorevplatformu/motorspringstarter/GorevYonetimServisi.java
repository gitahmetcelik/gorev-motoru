package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gorev API'nin iptal/yeniden-dene istekleri icin is kurallari. Iptal, API'nin hangi
 * worker instance'inin gorevi calistirdigini bilmesine gerek kalmadan calisir: gorev
 * henuz kuyrukta ise dogrudan durum gecisi yapilir, calisiyorsa sadece DB'de bir bayrak
 * isaretlenir (bkz IptalIzleyici).
 */
@Component
public class GorevYonetimServisi {

    private final GorevRepository gorevRepository;
    private final OluMektupKutusuRepository oluMektupKutusuRepository;
    private final OluMektupKutusuServisi oluMektupKutusuServisi;

    public GorevYonetimServisi(GorevRepository gorevRepository,
                               OluMektupKutusuRepository oluMektupKutusuRepository,
                               OluMektupKutusuServisi oluMektupKutusuServisi) {
        this.gorevRepository = gorevRepository;
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
        this.oluMektupKutusuServisi = oluMektupKutusuServisi;
    }

    // saveAndFlush kasten kullaniliyor: worker'in isle() metodu handler calisirken tum sure
    // boyunca ayni transaction'i acik tutuyor, bu yuzden burasi ile worker'in gec gelen commit'i
    // ayni gorev satirinda es zamanli olabilir. flush(), versiyon uyusmazligini (varsa) hemen,
    // bu metod donerken degil, cagiran tarafin (GorevController) yakalayip bir kez daha
    // deneyebilecegi sekilde firlatir.
    @Transactional
    public void iptalIste(UUID gorevId) {
        Gorev gorev = gorevRepository.findById(gorevId)
                .orElseThrow(() -> new GorevBulunamadiException(gorevId));
        switch (gorev.getDurum()) {
            case BEKLIYOR, KUYRUKTA -> gorev.durumGecisYap(GorevDurumu.IPTAL_EDILDI);
            case CALISIYOR -> gorev.iptalIstendiOlarakIsaretle();
            default -> throw new IllegalStateException(
                    "Bu durumdaki (" + gorev.getDurum() + ") gorev iptal edilemez: " + gorevId);
        }
        gorevRepository.saveAndFlush(gorev);
    }

    @Transactional
    public void yenidenDene(UUID gorevId) {
        if (!gorevRepository.existsById(gorevId)) {
            throw new GorevBulunamadiException(gorevId);
        }
        OluMektupKutusu kayit = oluMektupKutusuRepository.findFirstByGorevIdOrderByGirisZamaniDesc(gorevId)
                .orElseThrow(() -> new IllegalStateException(
                        "Bu gorev icin yeniden denenebilecek bir olu mektup kutusu kaydi yok: " + gorevId));
        if (kayit.isYenidenGonderildiMi()) {
            throw new IllegalStateException(
                    "Bu olu mektup kutusu kaydi zaten yeniden gonderildi: " + kayit.getId());
        }
        oluMektupKutusuServisi.yenidenGonder(kayit.getId());
    }
}
