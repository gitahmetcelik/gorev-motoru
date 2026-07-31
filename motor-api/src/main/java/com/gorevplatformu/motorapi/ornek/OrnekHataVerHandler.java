package com.gorevplatformu.motorapi.ornek;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;

@GorevTipi(value = "ornek.hata-ver", maxDeneme = 3)
public class OrnekHataVerHandler implements GorevHandler<OrnekHataVerHandler.Payload> {

    public record Payload(String sebep) {
    }

    @Override
    public Class<Payload> payloadTipi() {
        return Payload.class;
    }

    @Override
    public Object calistir(Payload payload, GorevBaglami baglam) {
        throw new RuntimeException("Kasitli hata: " + payload.sebep());
    }
}
