package com.gorevplatformu.motorcekirdek;

public interface GorevHandler<P> {

    Class<P> payloadTipi();

    Object calistir(P payload, GorevBaglami baglam) throws Exception;
}
