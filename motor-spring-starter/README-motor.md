# motor-spring-starter

Görev/iş kuyruğu motorunun Spring Boot 3 auto-configuration starter'ı. Postgres + RabbitMQ
üzerine ince bir katman kurar (bkz [ADR-0001](../docs/adr/0001-kutuphane-ustune-ince-katman.md)):
retry+backoff, DLQ, idempotency, öncelik kuyrukları, ShedLock ile zamanlanmış görevler,
işbirlikçi iptal, ve gözlemlenebilirlik (trace id, Micrometer metrikleri).

Faz 5 itibarıyla **Motor v1.0 donduruldu** — bu modül artık kararlı kabul ediliyor,
üzerine yeni domain servisleri (finans/bildirim/bakım) kuruluyor.

## Hızlı başlangıç

`pom.xml`'e ekle:

```xml
<dependency>
    <groupId>com.gorevplatformu</groupId>
    <artifactId>motor-spring-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Starter kendi kendine yeterlidir: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
üzerinden otomatik yüklenir, tüketici uygulamanın `scanBasePackages`/`@EnableJpaRepositories`/
`@EntityScan` ile elle dahil etmesine gerek yoktur.

**Bilinen sınırlama:** `motor` şemasının Flyway migration'ları (`V1`-`V5`) şu an
`motor-api` modülünde duruyor, starter'ın kendisinde değil — tüketici uygulama bu migration
dosyalarını kendi `db/migration` klasörüne kopyalamalı (veya `motor-api`'yi çalıştırılabilir
jar olarak kullanmalı).

### Zorunlu `application.yml` alanları

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gorev_platformu
    username: gorev
    password: ...
  flyway:
    schemas: motor
    default-schema: motor
  rabbitmq:
    host: localhost
    port: 5672
    username: gorev
    password: ...

motor:
  worker:
    tuketici-aktif: true   # bu instance'in oncelik kuyruklarini gercekten tuketip tuketmeyecegi
```

RabbitMQ, `x-delayed-message` plugin'ini destekleyen bir image gerektirir (bkz
`docker-compose.yml`, `heidiks/rabbitmq-delayed-message-exchange`) — retry backoff ve
gecikmeli görevler bu plugin'e dayanıyor.

## Handler yazma

```java
@GorevTipi(value = "ornek.echo", maxDeneme = 5, timeoutSaniye = 60, oncelik = 0)
public class OrnekEchoHandler implements GorevHandler<OrnekEchoHandler.Payload> {

    public record Payload(String mesaj) {}

    @Override
    public Class<Payload> payloadTipi() { return Payload.class; }

    @Override
    public Object calistir(Payload payload, GorevBaglami baglam) {
        baglam.iptalEdilirseFirlat(); // uzun suren islerde periyodik olarak cagrilmali
        return payload;
    }
}
```

`@GorevTipi` anotasyonlu her `GorevHandler` bean'i otomatik bulunup `gorev_tanimlari`
tablosuna senkronlanır.

## Görev gönderme

```java
UUID gorevId = gorevGonderici.gonder(
        "ornek.echo",
        new OrnekEchoHandler.Payload("merhaba"),
        new GorevOpsiyonlari(idempotencyAnahtari, oncelik, planlananZaman));
```

`idempotencyAnahtari` zorunludur; aynı anahtarla ikinci çağrı yeni görev oluşturmaz, mevcut
görevin id'sini döner. `oncelik` (>0 yüksek, 0 normal, <0 düşük) ve `planlananZaman`
(gecikmeli/zamanlanmış dispatch) opsiyoneldir.

## Nasıl çalışır

- **Idempotency** — iki katmanlı (bkz [ADR-0002](../docs/adr/0002-mesajlasma-katmani-rabbitmq.md)):
  gönderimde `idempotency_anahtari` UNIQUE + dedup, worker'da ise mesaj tekrar teslim edilirse
  (at-least-once) durum kontrolüyle sessizce atlanır.
- **Retry** — exponential backoff + jitter, mevcut transactional outbox'a (bkz
  [ADR-0004](../docs/adr/0004-transactional-outbox.md)) `x-delayed-message` ile entegre;
  yeni bir "gecikme" alt sistemi yok.
- **Öncelik kuyrukları** — 3 fiziksel kuyruk (yüksek/normal/düşük), tek bir özel tüketici
  (`GorevOncelikliTuketici`) senkron `basicGet` ile önce yükseğe bakar; strict-priority
  garantisi verir, `@RabbitListener` push-modelinin veremeyeceği bir garanti.
- **Zamanlanmış görevler** — `zamanlanmis_gorevler` tablosu + `ZamanlanmisGorevCalistirici`,
  ShedLock ile çoklu instance'ta tek çalıştırma garantisi (bkz
  [ADR-0003](../docs/adr/0003-tek-instance-iki-sema.md) tek-Postgres mimarisiyle tutarlı).
- **İşbirlikçi iptal** — API hangi worker instance'ının görevi çalıştırdığını bilmeden çalışır:
  DB'de `iptal_istendi` bayrağı işaretlenir, her worker'ın kendi `IptalIzleyici`'si kendi
  çalışan görevlerini kontrol eder.

## Gözlemlenebilirlik

- **Trace id** — Micrometer Tracing (Brave) ile HTTP isteklerinde otomatik üretilir, görev
  oluşturulurken `Gorev.traceId`'ye yazılır, mesaj zarfına eklenir, worker mesajı işlerken
  MDC'ye geri konur — bir görevin tüm log satırları (HTTP isteğinden handler'a kadar) aynı
  `traceId` alanını taşır.
- **Yapısal JSON log** — `logstash-logback-encoder`, MDC (traceId dahil) otomatik JSON alanı.
- **Micrometer metrikleri** (tüketici uygulama `spring-boot-starter-actuator` +
  `micrometer-registry-prometheus` eklemeli, starter sadece `MeterRegistry` API'sine bağımlı):
  - `gorev.kuyruk.derinlik{kuyruk=yuksek|normal|dusuk|dlq}` — canlı kuyruk derinliği
  - `gorev.isleme.suresi{tip,sonuc}` — p50/p95 (tüketici `management.metrics.distribution.percentiles`
    ile açmalı)
  - `gorev.sonuc{tip,sonuc}` — başarılı/başarısız/iptal sayaçları
  - `gorev.yeniden_deneme{tip}` — retry sayacı

## Sınırlamalar / sonraki fazlara notlar

- `motor-api`'nin component-scan'i artık sadece kendi paketini (`com.gorevplatformu.motorapi`)
  tarıyor; starter'ın tamamı auto-configuration ile geliyor. `finans-servisi`/`bildirim-servisi`/
  `bakim-servisi` bugün boş — Faz 6+'da bu modüllere gerçek `@Component`/`@Entity` eklenince,
  ya her domain modülü kendi auto-configuration'ını yazmalı, ya da `motor-api`'nin scan'i
  genişletilmeli.
- Flyway migration'ları starter'da değil `motor-api`'de duruyor (yukarıdaki "bilinen sınırlama").
- Exactly-once semantiği yok, at-least-once + idempotency ile yaklaşılıyor (bkz
  `Bilinçli Kapsam Dışı.md`).
