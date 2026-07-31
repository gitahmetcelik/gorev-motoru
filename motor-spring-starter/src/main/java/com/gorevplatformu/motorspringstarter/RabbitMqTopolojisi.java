package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMqTopolojisi {

    public static final String EXCHANGE = "gorev.exchange";
    public static final String GECIKMELI_EXCHANGE = "gorev.gecikmeli-exchange";
    public static final String ROUTING_KEY_ONEKI = "gorev.";

    public static final String SEGMENT_YUKSEK = "yuksek";
    public static final String SEGMENT_NORMAL = "normal";
    public static final String SEGMENT_DUSUK = "dusuk";

    public static final String KUYRUK_YUKSEK = "gorev.kuyruk.yuksek";
    public static final String KUYRUK_NORMAL = "gorev.kuyruk.normal";
    public static final String KUYRUK_DUSUK = "gorev.kuyruk.dusuk";

    public static final String DLX = "gorev.dlx";
    public static final String DLQ = "gorev.dlq";

    public static String oncelikSegmenti(int oncelik) {
        if (oncelik > 0) {
            return SEGMENT_YUKSEK;
        }
        if (oncelik < 0) {
            return SEGMENT_DUSUK;
        }
        return SEGMENT_NORMAL;
    }

    public static String kuyrukAdi(int oncelik) {
        return switch (oncelikSegmenti(oncelik)) {
            case SEGMENT_YUKSEK -> KUYRUK_YUKSEK;
            case SEGMENT_DUSUK -> KUYRUK_DUSUK;
            default -> KUYRUK_NORMAL;
        };
    }

    @Bean
    public TopicExchange gorevExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public CustomExchange gorevGecikmeliExchange() {
        return new CustomExchange(GECIKMELI_EXCHANGE, "x-delayed-message", true, false,
                Map.of("x-delayed-type", "topic"));
    }

    @Bean
    public DirectExchange gorevDlx() {
        return new DirectExchange(DLX, true, false);
    }

    private Queue oncelikliKuyrukOlustur(String ad) {
        return QueueBuilder.durable(ad)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue gorevKuyrukYuksek() {
        return oncelikliKuyrukOlustur(KUYRUK_YUKSEK);
    }

    @Bean
    public Queue gorevKuyrukNormal() {
        return oncelikliKuyrukOlustur(KUYRUK_NORMAL);
    }

    @Bean
    public Queue gorevKuyrukDusuk() {
        return oncelikliKuyrukOlustur(KUYRUK_DUSUK);
    }

    @Bean
    public Queue gorevDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    private Binding segmentBindingOlustur(Queue kuyruk, TopicExchange borsa, String segment) {
        return BindingBuilder.bind(kuyruk).to(borsa).with(ROUTING_KEY_ONEKI + segment + ".#");
    }

    @Bean
    public Binding gorevYuksekBinding() {
        return segmentBindingOlustur(gorevKuyrukYuksek(), gorevExchange(), SEGMENT_YUKSEK);
    }

    @Bean
    public Binding gorevNormalBinding() {
        return segmentBindingOlustur(gorevKuyrukNormal(), gorevExchange(), SEGMENT_NORMAL);
    }

    @Bean
    public Binding gorevDusukBinding() {
        return segmentBindingOlustur(gorevKuyrukDusuk(), gorevExchange(), SEGMENT_DUSUK);
    }

    @Bean
    public Binding gorevYuksekGecikmeliBinding() {
        return BindingBuilder.bind(gorevKuyrukYuksek()).to(gorevGecikmeliExchange())
                .with(ROUTING_KEY_ONEKI + SEGMENT_YUKSEK + ".#").noargs();
    }

    @Bean
    public Binding gorevNormalGecikmeliBinding() {
        return BindingBuilder.bind(gorevKuyrukNormal()).to(gorevGecikmeliExchange())
                .with(ROUTING_KEY_ONEKI + SEGMENT_NORMAL + ".#").noargs();
    }

    @Bean
    public Binding gorevDusukGecikmeliBinding() {
        return BindingBuilder.bind(gorevKuyrukDusuk()).to(gorevGecikmeliExchange())
                .with(ROUTING_KEY_ONEKI + SEGMENT_DUSUK + ".#").noargs();
    }

    @Bean
    public Binding gorevDlqBinding() {
        return BindingBuilder.bind(gorevDlq()).to(gorevDlx()).with(DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter mesajDonusturucu(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
