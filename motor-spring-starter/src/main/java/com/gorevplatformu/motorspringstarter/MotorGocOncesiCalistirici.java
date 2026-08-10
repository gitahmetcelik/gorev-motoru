package com.gorevplatformu.motorspringstarter;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Faz 9 (ecommerce-hub-plani-v5.md §Faz 9): motorun kendi migration'larini kendi ozel Flyway
 * tarihinde ({@code motor_schema_history}) calistirir — tuketicinin kendi Flyway zincirinden
 * tamamen bagimsiz.
 *
 * <p><b>Neden bir {@code @Bean Flyway} degil de bir {@link ApplicationContextInitializer}:</b>
 * Spring Boot'un {@code FlywayAutoConfiguration}'i {@code @ConditionalOnMissingBean(Flyway.class)}
 * tasir — tipi {@code Flyway} olan HERHANGI bir bean varsa Boot kendi Flyway bean'ini hic
 * olusturmaz. Starter kendi {@code Flyway} bean'ini kaydetseydi, tuketicinin (hub/webhook) kendi
 * migration zinciri sessizce hic calismazdi. Bu sinif hicbir {@code Flyway} tipinde bean
 * kaydetmez — {@link Flyway} nesnesini dogrudan, bir Spring bean'i olarak degil, bu metodun
 * icinde kurup calistirir.
 *
 * <p>{@link ApplicationContextInitializer#initialize} {@code SpringApplication.prepareContext()}
 * sirasinda, yani {@code refresh()} / bean tanimi yuklemesi baslamadan ONCE calisir — bu yuzden
 * Boot'un kendi Flyway auto-configuration'iyla hicbir sekilde carpismaz veya yaris durumuna
 * girmez, sira garantisi bean graph'a degil bu yasam dongusu asamasina dayanir. (Bu yaklasim bir
 * atilabilir spike ile dogrulandi: {@code @DynamicPropertySource} ile enjekte edilen
 * Testcontainers JDBC URL'i bu noktada zaten Environment'ta goruluyor — hem hub'in hem
 * webhook-platformu'nun gercek test/production yapilandirmasinin kullandigi mekanizma bu, {@code
 * @ServiceConnection} degil.)
 *
 * <p>{@code META-INF/spring.factories} ile kayitli — starter'i classpath'ine ekleyen her tuketici
 * bunu otomatik alir, ekstra yapilandirma gerekmez (ADR-0001'in "kendi kendine yeten starter"
 * felsefesiyle tutarli).
 */
public class MotorGocOncesiCalistirici implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(MotorGocOncesiCalistirici.class);

    /** V1-V5, tuketicinin eski paylasilan Flyway tarihinde zaten uygulanmis kabul edilir. */
    private static final String BASELINE_VERSIYON = "5";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        // BILINCLI OLARAK spring.flyway.enabled DEGIL: o, tuketicinin KENDI migration'ini
        // kapatip acmasi icindir (orn. motor-api Faz 9'dan sonra bunu false yapiyor, cunku kendi
        // migration'i artik yok) - motorunkiyle karistirilirsa, tuketici kendi migration'ini
        // kapatinca motor de sessizce kapanir, ki bu iki ayri karar. Ayri anahtar.
        boolean motorFlywayEnabled = env.getProperty("motor.flyway.enabled", Boolean.class, true);
        String url = env.getProperty("spring.datasource.url");
        if (!motorFlywayEnabled || url == null || url.isBlank()) {
            log.debug("motor: motor.flyway.enabled=false veya spring.datasource.url yok — "
                    + "motorun ozel Flyway calismasi atlaniyor");
            return;
        }

        String user = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        Flyway motorFlyway = Flyway.configure(getClass().getClassLoader())
                .dataSource(url, user, password)
                .locations("classpath:db/motor-migration")
                .schemas("motor")
                .defaultSchema("motor")
                .table("motor_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion(BASELINE_VERSIYON)
                .baselineDescription("motor v5-oncesi ortak Flyway tarihinde zaten uygulanmisti")
                .load();

        var sonuc = motorFlyway.migrate();
        log.info("motor: ozel Flyway calismasi tamamlandi — {} yeni migration uygulandi (hedef semaVersiyon={})",
                sonuc.migrationsExecuted, sonuc.targetSchemaVersion);
    }
}
