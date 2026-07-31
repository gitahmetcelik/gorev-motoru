package com.gorevplatformu.motorcekirdek;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GorevBaglamiImpl implements GorevBaglami {

    private final AtomicBoolean iptalTalepEdildi = new AtomicBoolean(false);

    public void iptalIste() {
        iptalTalepEdildi.set(true);
    }

    @Override
    public boolean iptalTalepEdildiMi() {
        return iptalTalepEdildi.get();
    }

    @Override
    public void iptalEdilirseFirlat() throws GorevIptalEdildiException {
        if (iptalTalepEdildi.get()) {
            throw new GorevIptalEdildiException();
        }
    }
}
