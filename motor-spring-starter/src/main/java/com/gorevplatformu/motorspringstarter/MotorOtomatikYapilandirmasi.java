package com.gorevplatformu.motorspringstarter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
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
 *
 * <p>Entity/repository taramasi iki ayri mekanizmayla, bilincli sirayla yapiliyor (ilk dis
 * tuketicide bulunan gercek bir hata, bkz gorev-motoru#feature/starter-tuketiciye-hazir):
 * <ul>
 *   <li>{@code @EntityScan} yerine {@link MotorEntityTaramaKayitEdici} kullaniliyor — duz
 *       {@code @EntityScan(basePackages=starter)}, {@code EntityScanPackages} bean'ini SADECE
 *       starter paketiyle kayit eder ve tuketicinin kendi entity paketini (normalde
 *       {@code AutoConfigurationPackages} fallback'iyle taranirdi) gorunmez kilardi. Registrar
 *       starter paketini tuketicinin taban paketiyle birlikte kaydediyor.</li>
 *   <li>{@code @EnableJpaRepositories} burada kaliyor ama {@code @AutoConfiguration(after=...)}
 *       ile Boot'un kendi {@code JpaRepositoriesAutoConfiguration}'indan SONRAYA aliniyor.
 *       Boot'un bu auto-configuration'i {@code @ConditionalOnMissingBean(JpaRepositoryFactoryBean)}
 *       tasiyor — starter kendi repository'lerini Boot'tan once kaydederse Boot'un tuketici icin
 *       yapacagi otomatik repository taramasi tamamen geri cekiliyordu. Once Boot'un kendi
 *       taramasi (tuketicinin paketini) calissin, sonra starter kendi paketini eklesin diye
 *       sira zorlaniyor.</li>
 * </ul>
 */
@AutoConfiguration(after = JpaRepositoriesAutoConfiguration.class)
@ComponentScan(basePackages = "com.gorevplatformu.motorspringstarter")
@Import(MotorEntityTaramaKayitEdici.class)
@EnableJpaRepositories(basePackages = "com.gorevplatformu.motorspringstarter")
@EnableScheduling
public class MotorOtomatikYapilandirmasi {
}
