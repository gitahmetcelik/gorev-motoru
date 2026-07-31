package com.gorevplatformu.motorspringstarter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Ana oncelik kuyruklarinda native RabbitMQ dead-lettering'e yol acan
 * mesajlari (orn. deserialize edilemeyen zehirli mesajlar) yakalar.
 * Is mantigi hatalari (handler exception) buraya asla dusmez, onlar
 * GorevMesajIsleyici icinde uygulama seviyesinde ele alinip
 * olu_mektup_kutusu'na dogrudan yazilir.
 */
@Component
public class GorevZehirliMesajDinleyicisi {

    private static final Logger log = LoggerFactory.getLogger(GorevZehirliMesajDinleyicisi.class);

    private final OluMektupKutusuRepository oluMektupKutusuRepository;

    public GorevZehirliMesajDinleyicisi(OluMektupKutusuRepository oluMektupKutusuRepository) {
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
    }

    @RabbitListener(queues = RabbitMqTopolojisi.DLQ, containerFactory = "gorevDlqListenerContainerFactory")
    @Transactional
    public void tuket(Message mesaj) {
        String hamGovde = new String(mesaj.getBody(), StandardCharsets.UTF_8);
        String hataSebebi = mesaj.getMessageProperties().getHeader("x-first-death-reason");
        log.warn("Zehirli/isleme-alinamayan mesaj DLQ'ya dustu: sebep={}, govde={}", hataSebebi, hamGovde);

        oluMektupKutusuRepository.save(OluMektupKutusu.zehirliMesaj(
                "Native RabbitMQ dead-letter: " + hataSebebi, hamGovde));
    }
}
