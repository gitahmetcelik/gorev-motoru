package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevBaglamiImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bu JVM'de o an calismakta olan gorevlerin GorevBaglami'larini tutar. Gorev API'deki
 * iptal endpoint'i hangi worker instance'inin gorevi calistirdigini bilemedigi icin
 * DB'ye sadece "iptal istendi" bayragi yazar (bkz Gorev.iptalIstendiOlarakIsaretle) —
 * her worker'daki IptalIzleyici bu bayragi kendi calisanGorevIdleri()'ndekilerle
 * karsilastirip lokal olarak iptalIste()'yi tetikler.
 */
@Component
public class CalisanGorevKayitDefteri {

    private final Map<UUID, GorevBaglamiImpl> calisanlar = new ConcurrentHashMap<>();

    public void basladi(UUID gorevId, GorevBaglamiImpl baglam) {
        calisanlar.put(gorevId, baglam);
    }

    public void bitti(UUID gorevId) {
        calisanlar.remove(gorevId);
    }

    public boolean iptalIste(UUID gorevId) {
        GorevBaglamiImpl baglam = calisanlar.get(gorevId);
        if (baglam == null) {
            return false;
        }
        baglam.iptalIste();
        return true;
    }

    public Set<UUID> calisanGorevIdleri() {
        return Set.copyOf(calisanlar.keySet());
    }
}
