package com.gorevplatformu.motorspringstarter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bir gorevin trace id'sini uretir: HTTP istegi sirasinda aktif bir span varsa onun
 * traceId'sini kullanir (Micrometer Tracing + Brave, Spring Boot HTTP istekleri icin bunu
 * otomatik acip MDC'ye yaziyor), yoksa (scheduler/demo gibi HTTP disinda tetiklenen gorevler
 * icin) yeni bir span acip kapatarak sadece id'sini alir. Tracer hic classpath'te yoksa (starter
 * tracing bridge'i zorunlu kilmiyor) duz bir UUID'ye duser.
 */
@Component
class TraceBaglamServisi {

    private final Tracer tracer;

    TraceBaglamServisi(ObjectProvider<Tracer> tracerSaglayici) {
        this.tracer = tracerSaglayici.getIfAvailable();
    }

    String mevcutYadaYeniTraceId() {
        if (tracer == null) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        Span mevcut = tracer.currentSpan();
        if (mevcut != null) {
            return mevcut.context().traceId();
        }
        Span yeni = tracer.nextSpan().name("gorev-olusturma").start();
        try {
            return yeni.context().traceId();
        } finally {
            yeni.end();
        }
    }
}
