package com.gorevplatformu.motorspringstarter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gorev motorunun Micrometer metrikleri. Kuyruk derinligi gauge'lari canli okunuyor (ayri bir
 * polling thread'i yok — Prometheus scrape ettigi anda RabbitMQ'ya passive declare ile sorulur).
 * Gercek HTTP/Prometheus endpoint'i (actuator) motor-api'de kaliyor, bu sinif sadece
 * MeterRegistry arayuzune bagimli.
 */
@Component
public class GorevMetrikleri {

    private final MeterRegistry meterRegistry;
    private final RabbitTemplate rabbitTemplate;

    public GorevMetrikleri(MeterRegistry meterRegistry, RabbitTemplate rabbitTemplate) {
        this.meterRegistry = meterRegistry;
        this.rabbitTemplate = rabbitTemplate;
        kuyrukGaugeKaydet(RabbitMqTopolojisi.KUYRUK_YUKSEK, "yuksek");
        kuyrukGaugeKaydet(RabbitMqTopolojisi.KUYRUK_NORMAL, "normal");
        kuyrukGaugeKaydet(RabbitMqTopolojisi.KUYRUK_DUSUK, "dusuk");
        kuyrukGaugeKaydet(RabbitMqTopolojisi.DLQ, "dlq");
    }

    private void kuyrukGaugeKaydet(String kuyrukAdi, String etiket) {
        Gauge.builder("gorev.kuyruk.derinlik", this, servis -> servis.kuyrukDerinligiOku(kuyrukAdi))
                .tag("kuyruk", etiket)
                .register(meterRegistry);
    }

    double kuyrukDerinligiOku(String kuyrukAdi) {
        try {
            Long sayim = rabbitTemplate.execute(channel -> channel.messageCount(kuyrukAdi));
            return sayim == null ? Double.NaN : sayim.doubleValue();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public Map<String, Long> kuyrukDerinlikleri() {
        Map<String, Long> derinlikler = new LinkedHashMap<>();
        derinlikler.put("yuksek", (long) kuyrukDerinligiOku(RabbitMqTopolojisi.KUYRUK_YUKSEK));
        derinlikler.put("normal", (long) kuyrukDerinligiOku(RabbitMqTopolojisi.KUYRUK_NORMAL));
        derinlikler.put("dusuk", (long) kuyrukDerinligiOku(RabbitMqTopolojisi.KUYRUK_DUSUK));
        derinlikler.put("dlq", (long) kuyrukDerinligiOku(RabbitMqTopolojisi.DLQ));
        return derinlikler;
    }

    void gorevSonuclandi(String tip, String sonuc, Duration sure) {
        meterRegistry.counter("gorev.sonuc", "tip", tip, "sonuc", sonuc).increment();
        meterRegistry.timer("gorev.isleme.suresi", "tip", tip, "sonuc", sonuc).record(sure);
    }

    void yenidenDenemeSayildi(String tip) {
        meterRegistry.counter("gorev.yeniden_deneme", "tip", tip).increment();
    }
}
