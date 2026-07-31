package com.gorevplatformu.motorspringstarter;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * CronExpression.next(Temporal), cron'un gun/ay/hafta-gunu gibi takvim alanlarini okuyabilmek icin
 * bir takvim bilgisi tasiyan Temporal ister (LocalDateTime/ZonedDateTime); Instant sadece bir epoch
 * ofsetidir, bu alanlari desteklemez ve UnsupportedTemporalTypeException fırlatir. Cron ifadeleri
 * burada hep UTC'de yorumlanir (sunucu saat dilimi degisikliklerinden etkilenmesin diye).
 */
final class ZamanlanmisGorevZamanHesaplayici {

    private ZamanlanmisGorevZamanHesaplayici() {
    }

    static Instant sonrakiniHesapla(String cronIfadesi, Instant referans) {
        LocalDateTime referansLocal = LocalDateTime.ofInstant(referans, ZoneOffset.UTC);
        LocalDateTime sonrakiLocal = CronExpression.parse(cronIfadesi).next(referansLocal);
        return sonrakiLocal == null ? null : sonrakiLocal.toInstant(ZoneOffset.UTC);
    }
}
