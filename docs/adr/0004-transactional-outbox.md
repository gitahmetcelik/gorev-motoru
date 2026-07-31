# ADR-004: Transactional outbox ile görev yayınlama

## Durum

Kabul edildi

## Bağlam

Bir domain işlemi (örn. "işlem kaydı") ile bunun tetiklediği görevin yayınlanması
("bütçe kontrol görevini yayınla") **atomik** olmalı. DB'ye yazmak ve broker'a publish
etmek iki farklı sistem; ikisi arasında uygulama çökerse ya görev kaybolur ya da DB'de
karşılığı olmayan bir "hayalet" görev broker'da dolaşır.

## Karar

Aynı transaction içinde iş kaydı ile birlikte `giden_mesajlar` (outbox) tablosuna da yaz.
Ayrı bir yayıncı (scheduled poller) bu tabloyu okuyup RabbitMQ'ya taşır ve
`yayinlandi_mi = true` olarak işaretler. Bu, Faz 2'de motor çekirdeğiyle birlikte
uygulanacak — sonradan eklemek tüm gönderim yolunun yeniden yazılması anlamına gelir,
bu yüzden temel bir kararın parçası.

## Sonuçlar

- Artı: DB yazımı ile mesaj yayını arasında veri kaybı ya da hayalet görev riski ortadan
  kalkıyor — ikisi tek transaction'ın parçası.
- Artı: Uygulama publish anında çökse bile (outbox yazıldıktan sonra, broker'a
  ulaşmadan) poller bir sonraki taramada mesajı yine de gönderir.
- Eksi: Ek bir tablo ve ek bir arka plan işlemi (poller) demek — basit bir "DB'ye yaz,
  sonra publish et" akışından daha karmaşık.
- Eksi: Poller'ın gecikmesi kadar (saniyeler mertebesinde) yayın gecikmesi oluşabilir;
  gerçek zamanlı değil, "az sonra tutarlı" (eventually consistent) bir model.
- İlişkili: At-least-once teslimat burada da geçerli — outbox poller aynı mesajı iki kez
  yayınlayabilir, bu yüzden idempotency (bkz. ADR-002) burada da savunma hattı.
