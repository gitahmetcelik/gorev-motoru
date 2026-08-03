# ADR-005: Starter'ın entity/repository taramasını tüketiciyle birleştirmesi

## Durum

Kabul edildi

## Bağlam

`motor-spring-starter` kendi entity/repository'lerini (`Gorev`, `GorevDenemesi`, ...)
görünür kılmak için `@EntityScan`/`@EnableJpaRepositories(basePackages = starter paketi)`
kullanıyordu. Bu, Spring Boot'un iki ayrı otomatik davranışını kırıyordu:

- `@EntityScan`, `EntityScanPackages` bean'ini SADECE starter paketiyle kaydediyor —
  Boot'un normalde tüketicinin ana `@SpringBootApplication` paketine geri düşen (fallback)
  entity taraması tamamen devre dışı kalıyor, tüketicinin kendi `@Entity`'leri görünmez
  oluyordu.
- `@EnableJpaRepositories`, Boot'un kendi `JpaRepositoriesAutoConfiguration`'ının
  `@ConditionalOnMissingBean(JpaRepositoryFactoryBean.class)` koşulunu tetikleyip geri
  çekilmesine yol açıyor — tüketicinin kendi repository'leri hiç taranmıyordu.

Bu, `motor-api`'de hiç fark edilmedi çünkü `motor-api`'nin kendi entity/repository'si yok.
İlk gerçek dış tüketici (bağımsız bir scratch tüketici projesiyle, `feature/starter-tuketiciye-hazir`
dalında) kendi `@Entity`/`@Repository`'lerini eklediğinde hatayı ortaya çıkardı.

## Karar

İki ayrı mekanizma, bilinçli sırayla:

1. `@EntityScan` yerine `MotorEntityTaramaKayitEdici` (bir `ImportBeanDefinitionRegistrar`)
   kullanılıyor — `AutoConfigurationPackages.get(beanFactory)` ile tüketicinin taban
   paketini okuyup starter paketiyle birlikte `EntityScanPackages.register(...)`'a veriyor.
   İkisi de taranıyor.
2. `MotorOtomatikYapilandirmasi`, `@AutoConfiguration(after = JpaRepositoriesAutoConfiguration.class)`
   ile Boot'un kendi repository taramasından SONRAYA alındı. Böylece Boot'un otomatik
   taraması önce tüketicinin repository'lerini kaydediyor (henüz hiçbir repository bean'i
   yokken koşuyor, koşulu geçiyor), starter kendi `@EnableJpaRepositories`'ini daha sonra
   uygulayıp sadece kendi paketini ekliyor.

Alternatif (tüketicide açık `@EntityScan`/`@EnableJpaRepositories({"tuketici", "starter"})`
deklarasyonu) daha basitti ama her tüketiciden tekrar tekrar aynı workaround'u istemek
yerine, hatayı kaynağında (starter'da) çözmek tercih edildi — starter'ın "jar'ı ekle, ekstra
konfigürasyon gerekmesin" vaadiyle tutarlı.

## Sonuçlar

- Artı: Tüketici hiçbir ekstra `@EntityScan`/`@EnableJpaRepositories` bildirmeden kendi
  entity/repository'lerini kullanabiliyor — starter gerçekten "jar'ı ekle, çalışsın"
  seviyesinde.
- Artı: Bağımsız bir scratch tüketici projesiyle (repoya girmedi) 3 kriterle doğrulandı:
  tüketici repository'si taranıyor, tüketici entity'si taranıyor, gönderilen görev
  `TAMAMLANDI` durumuna geçiyor.
- Eksi: `@AutoConfigureAfter`/registrar sırası, Spring Boot'un iç auto-configuration
  sıralama mekanizmasına (`AutoConfigurationSorter`) bağımlı — ileride Boot'un bu iç
  davranışı değişirse yeniden doğrulanması gerekir.
- İlişkili: Aynı dalda, `GorevTipiKayitDefteri`'nin katalog senkronu da benzer bir "ilk dış
  tüketicide ortaya çıkan" zamanlama hatası içeriyordu (bkz commit mesajı) — ikisi de
  "starter'ı sadece motor-api tüketiyordu, varsayımlar test edilmemişti" kategorisinde.
