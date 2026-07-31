# ADR-002: Mesajlaşma katmanı olarak RabbitMQ

## Durum

Kabul edildi

## Bağlam

Görev motoru bir kuyruk sistemine ihtiyaç duyuyor: öncelik kuyrukları, TTL, dead-letter
yönlendirme, gecikmeli (planlanmış) mesaj teslimi. Alternatif olarak Kafka değerlendirildi.

## Karar

RabbitMQ kullanılacak (yönetim arayüzü + `rabbitmq_delayed_message_exchange` eklentisi).

Kafka bir event stream'dir; kuyruk semantiği, retry ve DLQ'yu üstüne elle kurmak gerekir.
RabbitMQ'da öncelik kuyruğu, TTL, DLX ve delayed message plugin kutudan çıkıyor — bunlar
motorun temel ihtiyaçları. Kafka'yı "kurumsal görünsün" diye seçmek yanlış gerekçe olurdu;
seçim, iş yükünün doğasına (görev kuyruğu, event stream değil) dayanıyor.

## Sonuçlar

- Artı: Öncelik kuyruğu, TTL, DLX, delayed exchange gibi ihtiyaçların hepsi RabbitMQ'nun
  yerleşik özellikleriyle karşılanıyor, elle inşa etmeye gerek yok.
- Artı: RabbitMQ'nun `basic.ack`/`basic.nack` modeli, motorun "en az bir kez teslimat" +
  idempotency stratejisiyle doğal olarak örtüşüyor.
- Eksi: At-least-once teslimat kaçınılmaz — bu yüzden idempotency motorun opsiyonel değil,
  **zorunlu** bir parçası (`gorevler.idempotency_anahtari` UNIQUE kısıtı + handler içinde
  guard, iki katmanlı savunma).
- Eksi: Kafka'nın sunduğu event-sourcing / yeniden oynatma (replay) yeteneği yok; bu proje
  için gerekli değil, ama gelecekte event yayını gerekirse ayrı bir karar konusu.
