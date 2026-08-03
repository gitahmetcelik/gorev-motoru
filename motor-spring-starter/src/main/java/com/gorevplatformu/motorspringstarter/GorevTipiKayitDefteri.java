package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class GorevTipiKayitDefteri {

    private final Map<String, GorevHandler<?>> handlerlar = new HashMap<>();
    private final Map<String, GorevTipi> anotasyonlar = new HashMap<>();
    private final GorevTanimiRepository gorevTanimiRepository;

    public GorevTipiKayitDefteri(ApplicationContext baglam, GorevTanimiRepository gorevTanimiRepository) {
        this.gorevTanimiRepository = gorevTanimiRepository;
        baglam.getBeansOfType(GorevHandler.class).values().forEach(this::kaydet);
    }

    private void kaydet(GorevHandler<?> handler) {
        GorevTipi anotasyon = AnnotationUtils.findAnnotation(handler.getClass(), GorevTipi.class);
        if (anotasyon == null) {
            throw new IllegalStateException(
                    "GorevHandler bean'i @GorevTipi anotasyonu olmadan tanimlanamaz: " + handler.getClass());
        }
        handlerlar.put(anotasyon.value(), handler);
        anotasyonlar.put(anotasyon.value(), anotasyon);
    }

    public GorevHandler<?> handlerBul(String tip) {
        GorevHandler<?> handler = handlerlar.get(tip);
        if (handler == null) {
            throw new IllegalArgumentException("Bilinmeyen gorev tipi: " + tip);
        }
        return handler;
    }

    public GorevTipi tanimBul(String tip) {
        GorevTipi anotasyon = anotasyonlar.get(tip);
        if (anotasyon == null) {
            throw new IllegalArgumentException("Bilinmeyen gorev tipi: " + tip);
        }
        return anotasyon;
    }

    // ApplicationReadyEvent DEGIL: CommandLineRunner/ApplicationRunner bean'leri
    // (bkz DemoGorevBaslatici, ve dis tuketicilerin kendi startup runner'lari)
    // SpringApplication.run() icinde ApplicationReadyEvent'ten ONCE calisir
    // (listeners.started() -> callRunners() -> listeners.ready()). Katalog senkronu
    // ApplicationReadyEvent'i beklerse, acilista hemen gorev gonderen bir runner
    // gorev_tanimlari FK ihlaline carpar (satir henuz yazilmamis olur) — ilk dis
    // tuketicide (scratch-tuketici kapi testi) tazeden bulundu. ApplicationStartedEvent,
    // context tamamen refresh olduktan (repository/Flyway dahil) hemen sonra ama
    // runner'lardan once atesleniyor, bu yarisin onune geciyor.
    @EventListener(ApplicationStartedEvent.class)
    @Transactional
    public void kataloguSenkronla() {
        for (Map.Entry<String, GorevTipi> girdi : anotasyonlar.entrySet()) {
            String tip = girdi.getKey();
            GorevTipi anotasyon = girdi.getValue();
            gorevTanimiRepository.upsertYoksa(tip, RabbitMqTopolojisi.kuyrukAdi(anotasyon.oncelik()),
                    anotasyon.oncelik(), anotasyon.maxDeneme(), anotasyon.timeoutSaniye(), Instant.now());
        }
    }
}
