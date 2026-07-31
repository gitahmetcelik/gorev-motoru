package com.gorevplatformu.motorcekirdek;

import java.util.concurrent.ThreadLocalRandom;

public final class YenidenDenemeGecikmeHesaplayici {

    private static final long TABAN_GECIKME_MS = 2000;
    private static final long MAKS_GECIKME_MS = 60_000;
    private static final double JITTER_ORANI = 0.2;

    private YenidenDenemeGecikmeHesaplayici() {
    }

    public static long hesapla(int basarisizDenemeNo) {
        long ussel = TABAN_GECIKME_MS * (1L << Math.max(0, basarisizDenemeNo - 1));
        long sinirli = Math.min(ussel, MAKS_GECIKME_MS);
        double jitterCarpani = 1.0 + (ThreadLocalRandom.current().nextDouble(-JITTER_ORANI, JITTER_ORANI));
        return Math.round(sinirli * jitterCarpani);
    }
}
