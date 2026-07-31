package com.gorevplatformu.motorapi.api.dto;

import com.gorevplatformu.motorspringstarter.OluMektupKutusu;

import java.time.Instant;
import java.util.UUID;

public record OluMektupKutusuCevabi(UUID id, UUID gorevId, String sonHata, String hamMesaj, Instant girisZamani,
                                     boolean yenidenGonderildiMi) {

    public static OluMektupKutusuCevabi olustur(OluMektupKutusu kayit) {
        return new OluMektupKutusuCevabi(kayit.getId(), kayit.getGorevId(), kayit.getSonHata(),
                kayit.getHamMesaj(), kayit.getGirisZamani(), kayit.isYenidenGonderildiMi());
    }
}
