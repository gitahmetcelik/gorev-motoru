package com.gorevplatformu.motorapi.api.dto;

import com.gorevplatformu.motorspringstarter.GorevDenemesi;

import java.time.Instant;
import java.util.UUID;

public record GorevDenemesiCevabi(UUID id, Integer denemeNo, Instant baslangic, Instant bitis, String durum,
                                   String hataMesaji, String workerKimlik) {

    public static GorevDenemesiCevabi olustur(GorevDenemesi deneme) {
        return new GorevDenemesiCevabi(deneme.getId(), deneme.getDenemeNo(), deneme.getBaslangic(),
                deneme.getBitis(), deneme.getDurum(), deneme.getHataMesaji(), deneme.getWorkerKimlik());
    }
}
