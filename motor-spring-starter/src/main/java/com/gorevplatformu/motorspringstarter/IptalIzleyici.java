package com.gorevplatformu.motorspringstarter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Her worker instance'inda calisir, sadece KENDI o an calistirdigi gorevleri
 * (CalisanGorevKayitDefteri) DB'deki iptal_istendi bayragiyla karsilastirir.
 * Boylece API'nin hangi worker'in gorevi calistirdigini bilmesine gerek kalmaz —
 * kontrol sinyali hep DB uzerinden (tek dogruluk kaynagi) akar.
 */
@Component
public class IptalIzleyici {

    private final GorevRepository gorevRepository;
    private final CalisanGorevKayitDefteri calisanGorevKayitDefteri;

    public IptalIzleyici(GorevRepository gorevRepository, CalisanGorevKayitDefteri calisanGorevKayitDefteri) {
        this.gorevRepository = gorevRepository;
        this.calisanGorevKayitDefteri = calisanGorevKayitDefteri;
    }

    @Scheduled(fixedDelay = 1500)
    @Transactional(readOnly = true)
    public void kontrolEt() {
        Set<UUID> calisanIdler = calisanGorevKayitDefteri.calisanGorevIdleri();
        if (calisanIdler.isEmpty()) {
            return;
        }
        List<UUID> iptalIstenenler = gorevRepository.iptalIstenenIdleriBul(calisanIdler);
        for (UUID id : iptalIstenenler) {
            calisanGorevKayitDefteri.iptalIste(id);
        }
    }
}
