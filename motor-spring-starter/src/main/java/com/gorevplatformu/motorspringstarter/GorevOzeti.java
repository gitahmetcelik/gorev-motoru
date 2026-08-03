package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import java.util.UUID;

/**
 * Bir gorevin durumunu ozetleyen kucuk, salt-okunur goruntu. Tuketici uygulamalarin kendi
 * domain tablosundan (orn. bir teslimat/adim kaydi) motora tek bir noktadan bakabilmesi icin —
 * gorev/gorev_denemeleri semasina dogrudan bagimli olmadan.
 */
public record GorevOzeti(UUID gorevId, GorevDurumu durum, int denemeSayisi, String sonHata, String traceId) {
}
