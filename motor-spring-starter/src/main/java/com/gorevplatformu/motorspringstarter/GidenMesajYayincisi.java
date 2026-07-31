package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class GidenMesajYayincisi {

    private static final Logger log = LoggerFactory.getLogger(GidenMesajYayincisi.class);

    private final GidenMesajRepository gidenMesajRepository;
    private final GorevRepository gorevRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public GidenMesajYayincisi(GidenMesajRepository gidenMesajRepository, GorevRepository gorevRepository,
                                RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.gidenMesajRepository = gidenMesajRepository;
        this.gorevRepository = gorevRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // Faz4'ten itibaren birden fazla instance (api + worker node'lari) ayni anda ayaktayken bu
    // metodun coklu instance'ta ayni unpublished satiri es zamanli okuyup IKI KEZ publish etmesini
    // (ShedLock olmadan hicbir engel yoktu) ShedLock ile engelliyoruz. Faz3'teki idempotency guard
    // (GorevMesajIsleyici) boyle bir cift-yayinin yol actigi ikinci teslimati zaten zararsizca
    // yutuyordu, ama bu israfi (ve baska yaris kosullarini) kokten onlemek daha dogrusu.
    @Scheduled(fixedDelay = 2000)
    @SchedulerLock(name = "gidenMesajYayinla", lockAtLeastFor = "PT1S", lockAtMostFor = "PT30S")
    @Transactional
    public void yayinla() {
        List<GidenMesaj> yayinlanmamislar = gidenMesajRepository.findByYayinlandiMiFalseOrderByOlusturulmaAsc();
        for (GidenMesaj mesaj : yayinlanmamislar) {
            Optional<Gorev> gorevOptional = gorevRepository.findById(mesaj.getGorevId());
            if (gorevOptional.isEmpty()) {
                continue;
            }
            yayinlaTek(mesaj, gorevOptional.get());
        }
    }

    private void yayinlaTek(GidenMesaj mesaj, Gorev gorev) {
        // Gorev, kuyruga girmeden once (henuz BEKLIYOR/YENIDEN_DENENECEK durumundayken, orn.
        // planlanan_zaman ile gecikmeli bir gorev) iptal edilmis olabilir. Boyle bir gorevde
        // artik KUYRUKTA'ya gecis gecersiz olur (GorevDurumGecisi bunu reddeder) - mesaji hic
        // yayinlamadan, sadece outbox kaydini "islendi" olarak isaretleyip sonsuz yeniden
        // deneme donguisunu (her 2sn'de bir ayni hatayi atma) engelliyoruz.
        if (gorev.getDurum() != GorevDurumu.BEKLIYOR && gorev.getDurum() != GorevDurumu.YENIDEN_DENENECEK) {
            log.info("Gorev artik yayinlanabilir durumda degil (durum={}), outbox kaydi atlanıyor: {}",
                    gorev.getDurum(), gorev.getId());
            mesaj.yayinlandiOlarakIsaretle();
            gidenMesajRepository.save(mesaj);
            return;
        }

        JsonNode payloadNode;
        try {
            payloadNode = objectMapper.readTree(gorev.getPayload());
        } catch (Exception e) {
            throw new IllegalStateException("Gorev payload'i parse edilemedi: " + gorev.getId(), e);
        }

        GorevMesaji zarf = new GorevMesaji(gorev.getId(), gorev.getTip(), payloadNode, gorev.getTraceId());
        String routingKey = RabbitMqTopolojisi.ROUTING_KEY_ONEKI
                + RabbitMqTopolojisi.oncelikSegmenti(gorev.getOncelik()) + "." + gorev.getTip();

        if (mesaj.getGecikmeMs() != null && mesaj.getGecikmeMs() > 0) {
            rabbitTemplate.convertAndSend(RabbitMqTopolojisi.GECIKMELI_EXCHANGE, routingKey, zarf,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", mesaj.getGecikmeMs());
                        return message;
                    });
        } else {
            rabbitTemplate.convertAndSend(RabbitMqTopolojisi.EXCHANGE, routingKey, zarf);
        }

        gorev.durumGecisYap(GorevDurumu.KUYRUKTA);
        mesaj.yayinlandiOlarakIsaretle();
        gidenMesajRepository.save(mesaj);
    }
}
