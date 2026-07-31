package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void kataloguSenkronla() {
        for (Map.Entry<String, GorevTipi> girdi : anotasyonlar.entrySet()) {
            String tip = girdi.getKey();
            if (gorevTanimiRepository.existsById(tip)) {
                continue;
            }
            GorevTipi anotasyon = girdi.getValue();
            gorevTanimiRepository.save(new GorevTanimi(tip, RabbitMqTopolojisi.kuyrukAdi(anotasyon.oncelik()),
                    anotasyon.oncelik(), anotasyon.maxDeneme(), anotasyon.timeoutSaniye()));
        }
    }
}
