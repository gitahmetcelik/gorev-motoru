package com.gorevplatformu.motorapi.ornek;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;

@GorevTipi("ornek.echo")
public class OrnekEchoHandler implements GorevHandler<OrnekEchoHandler.Payload> {

    public record Payload(String mesaj) {
    }

    @Override
    public Class<Payload> payloadTipi() {
        return Payload.class;
    }

    @Override
    public Object calistir(Payload payload, GorevBaglami baglam) {
        return payload;
    }
}
