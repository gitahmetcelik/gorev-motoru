package com.gorevplatformu.motorspringstarter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * motor-spring-starter'in kendi kendine yeten bir Spring Boot starter'i olmasini saglayan
 * konsolide auto-configuration girisi. Starter'daki ~15 duz {@code @Component}/{@code @Repository}/
 * {@code @Entity}'yi tek tek {@code @Import} etmek yerine kendi dar paketini tariyor — bu paket
 * disina hic tasmadigi icin risk kabul edilebilir (bkz README-motor.md, ADR-0001).
 * Altyapi-karari gerektiren 3 sinif (RabbitMqTopolojisi, ShedLockYapilandirmasi,
 * GorevListenerContainerYapilandirmasi) ConditionalOnClass/ConditionalOnMissingBean
 * ifade edebilmek icin ayri auto-configuration olarak kaliyor.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.gorevplatformu.motorspringstarter")
@EnableJpaRepositories(basePackages = "com.gorevplatformu.motorspringstarter")
@EntityScan(basePackages = "com.gorevplatformu.motorspringstarter")
@EnableScheduling
public class MotorOtomatikYapilandirmasi {
}
