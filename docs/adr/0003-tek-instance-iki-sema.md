# ADR-003: Tek PostgreSQL instance, motor ve domain için ayrı şema

## Durum

Kabul edildi

## Bağlam

Motor (`motor` şeması) ve domain verisi (`finans` şeması) ayrı yaşam döngülerine sahip:
motor alan-bağımsız kalmalı, domain ise iş mantığına özgü. İki ayrı PostgreSQL container mı,
tek container'da iki şema mı sorusu var.

## Karar

Tek PostgreSQL container, `motor` ve `finans` için ayrı şema. Cross-schema foreign key
**yasak** — motor ile domain arasındaki tek bağ, `payload` içindeki id'ler (örn. `islemId`).
Motor bu id'leri opak bir değer olarak taşır, doğrulamaz.

İki ayrı container, bu ölçekte gereksiz operasyonel yük (iki bağlantı havuzu, iki
healthcheck, iki yedekleme stratejisi) getirir. Şema ayrımı, mantıksal izolasyonu tek
container'la sağlıyor.

## Sonuçlar

- Artı: Motoru ileride ayrı bir veritabanına taşımak mekanik bir işlem kalıyor (FK
  olmadığı için veri bütünlüğünü bozan bir bağımlılık yok).
- Artı: Geliştirme ortamı basit — tek `postgres` servisi, tek bağlantı dizesi ailesi
  (farklı `search_path`/şema ile).
- Eksi: Veritabanı seviyesinde referans bütünlüğü (FK) motor-domain arası garanti edilmiyor;
  tutarlılık uygulama katmanında (motor `islemId`'yi doğrulamadan taşır) sağlanıyor. Bu
  bilinçli bir ödünleşim — motorun domain'i asla bilmemesi kuralının bir sonucu.
