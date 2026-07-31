package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.GetResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Uc fiziksel oncelik kuyrugunu (yuksek/normal/dusuk) kesin siraya gore tuketir.
 * <p>
 * {@code @RabbitListener} push-modelinde her kuyruk icin bagimsiz bir container calisir —
 * biri bosken digeri mesgulken bile kendi mesajini ceker, bu strict-priority garantisi
 * VERMEZ. Burada tek arka plan thread'i, senkron {@code basicGet} ile her dongude once
 * yuksek kuyruga bakar; bir mesaj isleyince dongu yeniden yuksekten baslar. Boylece yuksek
 * oncelikli bir gorev geldigi an, o an islenmekte olan mesaj biter bitmez oncelik siralamasinda
 * one gecer.
 */
@Component
@ConditionalOnProperty(name = "motor.worker.tuketici-aktif", havingValue = "true", matchIfMissing = true)
public class GorevOncelikliTuketici {

    private static final Logger log = LoggerFactory.getLogger(GorevOncelikliTuketici.class);

    private static final List<String> ONCELIK_SIRASI = List.of(
            RabbitMqTopolojisi.KUYRUK_YUKSEK, RabbitMqTopolojisi.KUYRUK_NORMAL, RabbitMqTopolojisi.KUYRUK_DUSUK);
    private static final long BOS_KUYRUK_BEKLEME_MS = 250;
    private static final long KAPANIS_BEKLEME_MS = 60_000;

    private final RabbitTemplate rabbitTemplate;
    private final GorevMesajIsleyici gorevMesajIsleyici;
    private final ObjectMapper objectMapper;

    private volatile boolean calisiyor = true;
    private Thread tuketiciThread;

    public GorevOncelikliTuketici(RabbitTemplate rabbitTemplate, GorevMesajIsleyici gorevMesajIsleyici,
                                   ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.gorevMesajIsleyici = gorevMesajIsleyici;
        this.objectMapper = objectMapper;
    }

    // ApplicationReadyEvent'te baslatiliyor (@PostConstruct'ta degil): RabbitAdmin kuyruk/exchange/binding
    // deklarasyonlarini ConnectionFactory'ye ilk baglanti kuruldugunda (context refresh sirasinda)
    // yapiyor, bu da ApplicationReadyEvent'ten hemen once tamamlaniyor. @PostConstruct ile baslatilirsa
    // bu tuketici thread'i kuyruklar henuz deklare edilmeden basicGet'e girip 404 NOT_FOUND ile patlayabiliyor.
    @EventListener(ApplicationReadyEvent.class)
    public void baslat() {
        tuketiciThread = new Thread(this::dongu, "gorev-oncelikli-tuketici");
        tuketiciThread.start();
    }

    private void dongu() {
        while (calisiyor) {
            boolean islendi = false;
            for (String kuyruk : ONCELIK_SIRASI) {
                if (!calisiyor) {
                    break;
                }
                if (birMesajDeneCek(kuyruk)) {
                    islendi = true;
                    break;
                }
            }
            if (!islendi) {
                sleepQuietly(BOS_KUYRUK_BEKLEME_MS);
            }
        }
    }

    private boolean birMesajDeneCek(String kuyrukAdi) {
        return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> {
            GetResponse yanit = channel.basicGet(kuyrukAdi, false);
            if (yanit == null) {
                return false;
            }
            long teslimEtiketi = yanit.getEnvelope().getDeliveryTag();
            try {
                GorevMesaji mesaj = objectMapper.readValue(yanit.getBody(), GorevMesaji.class);
                gorevMesajIsleyici.isle(mesaj);
                channel.basicAck(teslimEtiketi, false);
            } catch (ObjectOptimisticLockingFailureException e) {
                // Gorev, bu mesaj islenirken API tarafinda es zamanli degistirildi (orn. iptal
                // edildi) - isle()'nin uzun suren transaction'i commit ederken versiyon
                // uyusmazligi yakalandi. Bu zararsiz bir yaris kaybi, DLQ'ya dusurmeye gerek yok.
                log.info("Gorev isleme sirasinda es zamanli degisti (optimistik kilit), mesaj ack'leniyor: "
                        + "kuyruk={}", kuyrukAdi);
                channel.basicAck(teslimEtiketi, false);
            } catch (Exception e) {
                log.error("Oncelikli tuketici mesaji islerken beklenmeyen hata, DLQ'ya dusuruluyor: kuyruk={}",
                        kuyrukAdi, e);
                channel.basicNack(teslimEtiketi, false, false);
            }
            return true;
        }));
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void durdur() throws InterruptedException {
        calisiyor = false;
        if (tuketiciThread != null) {
            tuketiciThread.join(KAPANIS_BEKLEME_MS);
        }
    }
}
