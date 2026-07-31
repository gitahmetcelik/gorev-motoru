package com.gorevplatformu.motorapi;

import com.gorevplatformu.motorapi.ornek.OrnekBekleHandler;
import com.gorevplatformu.motorapi.ornek.OrnekEchoHandler;
import com.gorevplatformu.motorapi.ornek.OrnekHataVerHandler;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.gorevplatformu.motorspringstarter.CalisanGorevKayitDefteri;
import com.gorevplatformu.motorspringstarter.GorevDenemesiRepository;
import com.gorevplatformu.motorspringstarter.GorevRepository;
import com.gorevplatformu.motorspringstarter.GorevYonetimServisi;
import com.gorevplatformu.motorspringstarter.OluMektupKutusu;
import com.gorevplatformu.motorspringstarter.OluMektupKutusuRepository;
import com.gorevplatformu.motorspringstarter.OluMektupKutusuServisi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Motorun (retry, DLQ, idempotency, isbirlikci iptal) gercek Postgres + RabbitMQ container'lariyla
 * uctan uca dogrulandigi entegrasyon testleri. Mevcut ornek.echo/bekle/hata-ver demo handler'lari
 * (motor-api'de, demo profili aktif olmadan da her zaman taraniyor) reuse ediliyor.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GorevMotoruEntegrasyonTestleri {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("heidiks/rabbitmq-delayed-message-exchange:3.13.0-management")
                    .asCompatibleSubstituteFor("rabbitmq"));

    @Autowired
    private GorevGonderici gorevGonderici;
    @Autowired
    private GorevRepository gorevRepository;
    @Autowired
    private GorevDenemesiRepository gorevDenemesiRepository;
    @Autowired
    private OluMektupKutusuRepository oluMektupKutusuRepository;
    @Autowired
    private OluMektupKutusuServisi oluMektupKutusuServisi;
    @Autowired
    private GorevYonetimServisi gorevYonetimServisi;
    @Autowired
    private CalisanGorevKayitDefteri calisanGorevKayitDefteri;

    @Test
    void retryDlqVeYenidenGonderme() {
        UUID gorevId = gorevGonderici.gonder("ornek.hata-ver",
                new OrnekHataVerHandler.Payload("entegrasyon-testi"),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), null, null));

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() ->
                assertThat(gorevRepository.findById(gorevId).orElseThrow().getDurum())
                        .isEqualTo(GorevDurumu.BASARISIZ));

        OluMektupKutusu kayit = oluMektupKutusuRepository.findFirstByGorevIdOrderByGirisZamaniDesc(gorevId)
                .orElseThrow();
        assertThat(kayit.isYenidenGonderildiMi()).isFalse();

        oluMektupKutusuServisi.yenidenGonder(kayit.getId());

        // ornek.hata-ver her zaman hata firlatir, yeniden gonderim de sonunda tekrar BASARISIZ'a duser -
        // burada dogrulanan yeniden gonderimin gercekten calistigi, mucize gibi basarili olmasi degil.
        await().atMost(Duration.ofSeconds(45)).untilAsserted(() ->
                assertThat(oluMektupKutusuRepository.findById(kayit.getId()).orElseThrow().isYenidenGonderildiMi())
                        .isTrue());
    }

    @Test
    void idempotencyDedupVeTekDeneme() {
        String anahtar = "entegrasyon-idempotency-" + UUID.randomUUID();
        GorevOpsiyonlari opsiyonlar = new GorevOpsiyonlari(anahtar, null, null);
        UUID birinci = gorevGonderici.gonder("ornek.echo", new OrnekEchoHandler.Payload("ilk"), opsiyonlar);
        UUID ikinci = gorevGonderici.gonder("ornek.echo", new OrnekEchoHandler.Payload("ikinci"), opsiyonlar);

        assertThat(ikinci).isEqualTo(birinci);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(gorevRepository.findById(birinci).orElseThrow().getDurum())
                        .isEqualTo(GorevDurumu.TAMAMLANDI));

        assertThat(gorevDenemesiRepository.findByGorevIdOrderByBaslangicAsc(birinci)).hasSize(1);
    }

    @Test
    void iptalHenuzCalismadan() {
        UUID gorevId = gorevGonderici.gonder("ornek.echo", new OrnekEchoHandler.Payload("gecikmeli"),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), null, Instant.now().plusSeconds(5)));

        gorevYonetimServisi.iptalIste(gorevId);

        assertThat(gorevRepository.findById(gorevId).orElseThrow().getDurum())
                .isEqualTo(GorevDurumu.IPTAL_EDILDI);

        // Gorevin normalde kuyruga girecegi (5sn) ve islenecegi zamani gecince de hic
        // deneme kaydi olusmadigini dogrula - idempotency guard/outbox skip'i sessizce calisti.
        await().pollDelay(Duration.ofSeconds(7)).atMost(Duration.ofSeconds(8)).until(() -> true);
        assertThat(gorevDenemesiRepository.findByGorevIdOrderByBaslangicAsc(gorevId)).isEmpty();
    }

    @Test
    void iptalCalisirken() {
        UUID gorevId = gorevGonderici.gonder("ornek.bekle", new OrnekBekleHandler.Payload(3),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), null, null));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> calisanGorevKayitDefteri.calisanGorevIdleri().contains(gorevId));

        gorevYonetimServisi.iptalIste(gorevId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(gorevRepository.findById(gorevId).orElseThrow().getDurum())
                        .isEqualTo(GorevDurumu.IPTAL_EDILDI));
    }
}
