package com.gorevplatformu.motorapi.api.dto;

import com.gorevplatformu.motorspringstarter.ZamanlanmisGorev;

import java.time.Instant;
import java.util.UUID;

public record ZamanlanmisGorevCevabi(UUID id, String tip, String cronIfadesi, boolean aktif,
                                      Instant sonCalisma, Instant sonrakiCalisma) {

    public static ZamanlanmisGorevCevabi olustur(ZamanlanmisGorev zamanlanmis) {
        return new ZamanlanmisGorevCevabi(zamanlanmis.getId(), zamanlanmis.getTip(), zamanlanmis.getCronIfadesi(),
                zamanlanmis.isAktif(), zamanlanmis.getSonCalisma(), zamanlanmis.getSonrakiCalisma());
    }
}
