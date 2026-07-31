package com.gorevplatformu.motorspringstarter;

import java.util.UUID;

public class GorevBulunamadiException extends RuntimeException {

    public GorevBulunamadiException(UUID gorevId) {
        super("Gorev bulunamadi: " + gorevId);
    }
}
