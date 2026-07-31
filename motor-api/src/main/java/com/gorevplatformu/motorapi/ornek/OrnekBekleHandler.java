package com.gorevplatformu.motorapi.ornek;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;

@GorevTipi(value = "ornek.bekle", timeoutSaniye = 5, maxDeneme = 1)
public class OrnekBekleHandler implements GorevHandler<OrnekBekleHandler.Payload> {

    private static final long ADIM_MS = 200;

    public record Payload(long saniye) {
    }

    @Override
    public Class<Payload> payloadTipi() {
        return Payload.class;
    }

    @Override
    public Object calistir(Payload payload, GorevBaglami baglam) throws InterruptedException {
        long toplamMs = payload.saniye() * 1000;
        long gecenMs = 0;
        while (gecenMs < toplamMs) {
            baglam.iptalEdilirseFirlat();
            Thread.sleep(Math.min(ADIM_MS, toplamMs - gecenMs));
            gecenMs += ADIM_MS;
        }
        return "tamamlandi";
    }
}
