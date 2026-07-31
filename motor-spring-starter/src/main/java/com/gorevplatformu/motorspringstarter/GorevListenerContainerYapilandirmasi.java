package com.gorevplatformu.motorspringstarter;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Faz4'ten itibaren tek gercek {@code @RabbitListener} tuketicisi DLQ dinleyicisi
 * (GorevZehirliMesajDinleyicisi) — ana oncelik kuyruklari artik push-model degil,
 * GorevOncelikliTuketici'nin senkron basicGet donguisuyle tuketiliyor (kendi
 * graceful-shutdown'ini kendi @PreDestroy'unda yapiyor). Bu yuzden burada sadece
 * DLQ icin ozel bir container factory kaliyor.
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass(SimpleRabbitListenerContainerFactory.class)
public class GorevListenerContainerYapilandirmasi {

    private static final long KAPANIS_BEKLEME_MS = 60_000;

    /**
     * DLQ dinleyicisi (GorevZehirliMesajDinleyicisi) ham org.springframework.amqp.core.Message
     * bekliyor, ama Jackson2JsonMessageConverter kullanan varsayilan factory yine de govdeyi
     * JSON'a cevirmeyi DENIYOR (converted payload once olusturuluyor, sonra Message tipine
     * geri dusuluyor) - bu yuzden zehirli/gecersiz JSON, dogru DLQ listener'ina bile
     * ulasmadan MessageConversionException firlatiyor. DLQ dinleyicisi icin donusturmesiz
     * (ham byte[]/String) ayri bir factory kullaniyoruz.
     */
    @Bean
    @ConditionalOnMissingBean(name = "gorevDlqListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory gorevDlqListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setContainerCustomizer(container -> container.setShutdownTimeout(KAPANIS_BEKLEME_MS));
        return factory;
    }
}
