# motor-spring-starter

Görev/iş kuyruğu motorunun Spring Boot 3 auto-configuration starter'ı. Postgres + RabbitMQ
üzerine ince bir katman kurar (bkz [ADR-0001](../docs/adr/0001-kutuphane-ustune-ince-katman.md)):
retry+backoff, DLQ, idempotency, öncelik kuyrukları, ShedLock ile zamanlanmış görevler,
işbirlikçi iptal, ve gözlemlenebilirlik (trace id, Micrometer metrikleri).

Starter'ın davranışı ve genel API'si kararlı kabul edilir; üzerine yeni bir domain
servisi kurmak isteyen bir tüketici, aşağıdaki adımları izleyerek entegre olabilir.

## Hızlı başlangıç

`pom.xml`'e ekle:

```xml
<dependency>
    <groupId>com.gorevplatformu</groupId>
    <artifactId>motor-spring-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Starter kendi kendine yeterlidir: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
üzerinden otomatik yüklenir. Tüketici uygulamanın kendi `@Component`/`@Entity`/`@Repository`'leri
normal Spring Boot davranışıyla (ana `@SpringBootApplication` sınıfının paketinden aşağı doğru)
taranır — starter'ın kendi paketini görünür kılmak için ekstra bir `scanBasePackages`/
`@EnableJpaRepositories`/`@EntityScan` bildirmeye gerek yoktur, starter ikisini de kendi
`MotorOtomatikYapilandirmasi`'nda birlikte kaydeder.

`motor` şemasının Flyway migration'ları (`V1`-`V5`) starter'ın kendi kaynaklarında
(`classpath:db/motor-migration`) taşınır — tüketici uygulama kendi migration'larıyla birlikte
aşağıdaki gibi iki konum belirtmeli (kopyalama gerekmez):

```yaml
spring:
  flyway:
    locations: classpath:db/motor-migration,classpath:db/migration
```

### Zorunlu `application.yml` alanları

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gorev_platformu
    username: gorev
    password: ...
  flyway:
    locations: classpath:db/motor-migration,classpath:db/migration
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

Tüketici kendi domain şemasını da kullanıyorsa (`spring.flyway.schemas: motor,<kendi-seman>`),
Flyway tüm konumlardaki migration'ları tek bir sürüm zincirinde uygular — `V1`-`V5` numaraları
motora ait olduğu için kendi migration'larına `V6`'dan başlaman gerekir.

RabbitMQ, `x-delayed-message` plugin'ini destekleyen bir image gerektirir (bkz
`docker-compose.yml`, `heidiks/rabbitmq-delayed-message-exchange`) — retry backoff ve
gecikmeli görevler bu plugin'e dayanıyor.

### Starter'ın getirmediği, tüketicinin kendi eklemesi gereken bağımlılıklar

Bağımsız bir scratch tüketiciyle (`0.1.0-SNAPSHOT` kapı testi) doğrulandı — starter bilerek
dar tutulmuş, aşağıdaki üç bağımlılık olmadan uygulama açılışta hata verir:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-json</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

- `flyway-core`+`flyway-database-postgresql` olmadan Flyway auto-configuration hiç devreye
  girmez (`@ConditionalOnClass(Flyway.class)`), `motor` şeması hiç oluşmaz, ilk JPA erişiminde
  "schema motor does not exist" hatası alınır.
- `spring-boot-starter-json` (veya tam web starter'ı) olmadan `Jackson2ObjectMapperBuilder`
  sınıfı (spring-web'de) bulunamaz, Boot'un varsayılan `ObjectMapper` bean'i oluşmaz —
  `RabbitMqTopolojisi`'nin mesaj dönüştürücüsü bunu ister.
- `spring-boot-starter-actuator` (veya başka bir `micrometer-registry-*`) olmadan somut bir
  `MeterRegistry` bean'i oluşmaz — starter sadece `micrometer-core` (arayüzler) taşıyor.

### Açılışta görev göndermeyle ilgili bir tuzak

`GorevOncelikliTuketici`'nin arka plan tüketici thread'i `ApplicationReadyEvent`'te başlar.
Spring Boot'ta `CommandLineRunner`/`ApplicationRunner` bean'leri **`ApplicationReadyEvent`'ten
önce** çalışır (`SpringApplication.run()` sırası: `started` → `callRunners` → `ready`). Bu
yüzden bir başlangıç runner'ı gönderdiği görevin sonucunu **aynı thread'de bloke olarak
bekliyorsa**, görevi tamamlayacak tüketici hiç başlamamış olur (kendi kendini kilitleme).
Görev gönderip sonucunu görmek isteyen bir runner, bekleme kısmını ayrı bir thread'e almalı.

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

## Sınırlamalar

- `GorevYonetimServisi`/`OluMektupKutusuServisi`'nin sunduğu yüzey dar (iptal/yeniden-dene/
  yeniden-gönder + `ozet(UUID)` sorgusu) — teslimat/iş geçmişinin tam görünürlüğü isteyen bir
  ürün kendi domain şemasında kendi kayıtlarını tutmalı, motor şemasına sıkı bağlanmamalı.
- Exactly-once semantiği yok, at-least-once + idempotency ile yaklaşılıyor — bu bilinçli bir
  tasarım tercihi, dağıtık sistemlerde exactly-once garantisinin getirdiği karmaşıklık bu
  motorun kapsamının dışında tutuldu.
- REST API, JWT güvenliği ve statik dashboard `motor-api` modülünde — starter'a bağımlılık
  bunları içermez, sadece motorun çalıştırma/kalıcılık katmanını verir.
