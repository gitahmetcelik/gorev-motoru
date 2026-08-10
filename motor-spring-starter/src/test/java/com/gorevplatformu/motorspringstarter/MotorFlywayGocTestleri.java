package com.gorevplatformu.motorspringstarter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Faz 9 (ecommerce-hub-plani-v5.md §9.4): motorun kendi ozel Flyway calismasini (bkz
 * {@link MotorGocOncesiCalistirici}) hem BOS bir DB'ye karsi hem de v5-oncesi seklin
 * ({@code motor.*} tablolari zaten var, ama {@code motor_schema_history} yok — eski paylasilan
 * {@code flyway_schema_history} tek tarihti) ustune GERCEKCI SEKILDE dogrular.
 *
 * <p>Ikinci senaryo (yukseltme) planin kendi ifadesiyle "asil risk" — motorun mevcut bir
 * DB'nin verisini kaybetmeden, eski tarihi silmeden, kendi ozel tarihine gecebilmesi.
 */
class MotorFlywayGocTestleri {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            FlywayAutoConfiguration.class,
            MotorOtomatikYapilandirmasi.class,
            RabbitMqTopolojisi.class,
            ShedLockYapilandirmasi.class,
            GorevListenerContainerYapilandirmasi.class
    })
    static class TestUygulamasi {
    }

    @Nested
    @SpringBootTest(classes = TestUygulamasi.class)
    @ContextConfiguration
    class BosDbTestleri {

        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

        static {
            postgres.start();
        }

        @DynamicPropertySource
        static void ozellikler(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Test
        void bosDbdeBesGercekMigrationCalisir() throws Exception {
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword());
                 Statement st = conn.createStatement()) {

                // Flyway kendi "SCHEMA" olusturma kaydini da (createSchemas=true, sema yeni
                // olusturuldugu icin) tarihe ekliyor - sadece gercek SQL migration satirlarini say.
                try (ResultSet rs = st.executeQuery(
                        "SELECT type FROM motor.motor_schema_history WHERE type = 'SQL'")) {
                    int sayi = 0;
                    while (rs.next()) {
                        sayi++;
                    }
                    assertThat(sayi).isEqualTo(5);
                }

                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM motor.gorev_tanimlari")) {
                    rs.next();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
        }
    }

    @Nested
    @SpringBootTest(classes = TestUygulamasi.class)
    @ContextConfiguration
    class VarOlanDbYukseltmeTestleri {

        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

        static {
            postgres.start();
            // v5-oncesi sekli elle kur: motor.* tablolari (bugunku semaya ozdes - eski
            // default-schema=motor zaten fiziksel olarak ayni yere yaziyordu) zaten var,
            // ESKI paylasilan flyway_schema_history'de V1-V5 kayitli, ama motor_schema_history
            // HENUZ YOK. MotorGocOncesiCalistirici'nin bu durumda baselineOnMigrate ile
            // (yeniden calistirmadan) uyum saglamasi gerekiyor.
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword());
                 Statement st = conn.createStatement()) {

                st.execute("CREATE SCHEMA motor");
                st.execute("""
                        CREATE TABLE motor.gorev_tanimlari (
                            tip TEXT PRIMARY KEY,
                            versiyon INT NOT NULL DEFAULT 1,
                            kuyruk TEXT NOT NULL,
                            varsayilan_oncelik INT NOT NULL DEFAULT 0,
                            varsayilan_retry INT NOT NULL DEFAULT 5,
                            timeout_sn INT NOT NULL DEFAULT 60,
                            olusturulma TIMESTAMPTZ NOT NULL DEFAULT now()
                        )""");
                // Eski DB'de gercek is verisi vardi - yukseltmenin bunu KAYBETMEDIGINI kanitlamak
                // icin bir marker satiri.
                st.execute("INSERT INTO motor.gorev_tanimlari (tip, kuyruk) "
                        + "VALUES ('yukseltme-oncesi-marker', 'varsayilan')");

                // Eski PAYLASILAN tarih tablosu (tuketicinin V1000+/V6+'siyla birlikte,
                // basitlestirilmis - sadece motorun V1-V5'ini temsil eden 5 satir).
                st.execute("""
                        CREATE TABLE flyway_schema_history (
                            installed_rank INT PRIMARY KEY,
                            version VARCHAR(50),
                            description VARCHAR(200),
                            script VARCHAR(1000),
                            success BOOLEAN
                        )""");
                for (int v = 1; v <= 5; v++) {
                    st.execute("INSERT INTO flyway_schema_history (installed_rank, version, description, script, success) "
                            + "VALUES (" + v + ", '" + v + "', 'eski-motor-migration-" + v + "', 'V" + v + "__eski.sql', true)");
                }
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("Test kurulumu: v5-oncesi sekli elle kurarken hata", e);
            }
        }

        @DynamicPropertySource
        static void ozellikler(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Test
        void varOlanDbdeYenidenCalistirmadanBaselineOlur() throws Exception {
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword());
                 Statement st = conn.createStatement()) {

                // 1) motor_schema_history TEK bir BASELINE satiriyla olustu, 5 SQL satiriyla degil.
                try (ResultSet rs = st.executeQuery("SELECT version, type FROM motor.motor_schema_history")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("version")).isEqualTo("5");
                    assertThat(rs.getString("type")).isEqualTo("BASELINE");
                    assertThat(rs.next()).isFalse();
                }

                // 2) Var olan veri KAYBOLMADI (V1 yeniden calisip tabloyu bosaltmadi/DROP etmedi).
                try (ResultSet rs = st.executeQuery(
                        "SELECT kuyruk FROM motor.gorev_tanimlari WHERE tip = 'yukseltme-oncesi-marker'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("kuyruk")).isEqualTo("varsayilan");
                }

                // 3) Eski PAYLASILAN tarih SILINMEDI - plan sart kosuyor: "baseline, silme degil".
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM flyway_schema_history")) {
                    rs.next();
                    assertThat(rs.getInt(1)).isEqualTo(5);
                }
            }
        }
    }
}
