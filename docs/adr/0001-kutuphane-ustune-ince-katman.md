# ADR-001: Motoru kütüphane üstüne ince bir katman olarak kurmak

## Durum

Kabul edildi

## Bağlam

Görev motoru; sıraya alma, retry/backoff, DLQ, zamanlama, dağıtık kilit gibi dağıtık sistem
problemlerini çözmek zorunda. Bunların hepsi tek tek elle (sıfırdan) yazılabilir, ama her biri
kendi başına iyi test edilmiş, üretimde kanıtlanmış kütüphaneler tarafından zaten çözülmüş
problemler: RabbitMQ (kuyruk, TTL, DLX, delayed exchange), Spring AMQP (tüketici yönetimi),
ShedLock (dağıtık kilit), Flyway (şema geçişi).

## Karar

`motor-cekirdek` sadece alan-bağımsız soyutlamaları taşır (`GorevHandler<P>`, `@GorevTipi`,
`GorevGonderici`, `GorevOpsiyonlari`, `GorevDurumu`, `GorevBaglami`) — hiçbir dağıtık sistem
primitifini yeniden uygulamaz.
`motor-spring-starter` bu soyutlamaları RabbitMQ/Spring/ShedLock gibi kütüphanelerin üzerine
ince bir auto-configuration katmanı olarak bağlar. Motor, "queue nasıl çalışır" sorusuna kendi
cevabını üretmez; bunun yerine RabbitMQ'nun cevabını kullanır ve üstüne sadece domain-agnostik
görev soyutlamasını ekler.

## Sonuçlar

- Artı: Dağıtık sistem hatalarına (mesaj kaybı, çift teslimat, kilitlenme) karşı kütüphanenin
  olgunluğuna güveniliyor; bu sınıfta hata yüzeyi küçülüyor.
- Artı: Bir domain modülü sadece `motor-spring-starter` bağımlılığını ekleyip `@GorevTipi`
  yazarak entegre olur — starter'ın "modülerliğin somut kanıtı" olma iddiası buradan gelir.
- Eksi: Motorun davranışı, sarmaladığı kütüphanelerin (özellikle RabbitMQ) davranış ve
  sınırlarına bağımlı kalıyor; kütüphane değişirse ince katman da değişmek zorunda.
- Bilinçli olarak dışarıda bırakıldı: Kafka'ya event yayını, exactly-once semantiği — bkz.
  README "Bilinen sınırlamalar" bölümü.
