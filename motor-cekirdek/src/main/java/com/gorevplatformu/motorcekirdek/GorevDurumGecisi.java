package com.gorevplatformu.motorcekirdek;

public final class GorevDurumGecisi {

    private GorevDurumGecisi() {
    }

    public static void dogrula(GorevDurumu mevcut, GorevDurumu hedef) {
        boolean gecerli = switch (mevcut) {
            // BEKLIYOR -> CALISIYOR: outbox yayin dongusu coklu mesaji tek transaction'da yayinlarken,
            // RabbitMQ tuketicisi commit'ten once mesaji yakalayabilir; bu asamada BEKLIYOR gormek
            // gecici bir gozlem gecikmesidir (mesaj zaten broker'a ulasti), gecersiz bir durum degil.
            case BEKLIYOR -> hedef == GorevDurumu.KUYRUKTA || hedef == GorevDurumu.IPTAL_EDILDI
                    || hedef == GorevDurumu.CALISIYOR;
            case KUYRUKTA -> hedef == GorevDurumu.CALISIYOR || hedef == GorevDurumu.IPTAL_EDILDI;
            case CALISIYOR -> hedef == GorevDurumu.TAMAMLANDI || hedef == GorevDurumu.BASARISIZ
                    || hedef == GorevDurumu.YENIDEN_DENENECEK || hedef == GorevDurumu.IPTAL_EDILDI;
            case YENIDEN_DENENECEK -> hedef == GorevDurumu.KUYRUKTA;
            case BASARISIZ -> hedef == GorevDurumu.BEKLIYOR;
            case TAMAMLANDI, IPTAL_EDILDI -> false;
        };
        if (!gecerli) {
            throw new IllegalStateException("Gecersiz durum gecisi: " + mevcut + " -> " + hedef);
        }
    }
}
