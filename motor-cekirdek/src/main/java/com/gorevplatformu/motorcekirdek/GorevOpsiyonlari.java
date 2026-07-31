package com.gorevplatformu.motorcekirdek;

import java.time.Instant;

public record GorevOpsiyonlari(String idempotencyAnahtari, Integer oncelik, Instant planlananZaman) {

    public GorevOpsiyonlari {
        if (idempotencyAnahtari == null || idempotencyAnahtari.isBlank()) {
            throw new IllegalArgumentException("idempotencyAnahtari zorunludur");
        }
        if (oncelik == null) {
            oncelik = 0;
        }
    }
}
