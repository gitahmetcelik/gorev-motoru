-- Faz 9 (ecommerce-hub-plani-v5.md): motor artik kendi ozel Flyway calismasinda, kendi
-- semasinda calisiyor. Once bu semanin var oldugundan emin ol - eskiden tuketicinin
-- "schemas" listesi + Flyway'in createSchemas'i bunu ustumuze aliyordu, artik almiyor.
CREATE SCHEMA IF NOT EXISTS motor;

CREATE TABLE motor.gorev_tanimlari (
    tip                 TEXT PRIMARY KEY,
    versiyon            INT NOT NULL DEFAULT 1,
    kuyruk              TEXT NOT NULL,
    varsayilan_oncelik  INT NOT NULL DEFAULT 0,
    varsayilan_retry    INT NOT NULL DEFAULT 5,
    timeout_sn          INT NOT NULL DEFAULT 60,
    olusturulma         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE motor.gorevler (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tip                  TEXT NOT NULL REFERENCES motor.gorev_tanimlari (tip),
    payload              JSONB NOT NULL,
    durum                TEXT NOT NULL DEFAULT 'BEKLIYOR'
                             CHECK (durum IN (
                                 'BEKLIYOR', 'KUYRUKTA', 'CALISIYOR',
                                 'TAMAMLANDI', 'BASARISIZ',
                                 'YENIDEN_DENENECEK', 'IPTAL_EDILDI'
                             )),
    idempotency_anahtari TEXT NOT NULL UNIQUE,
    oncelik              INT NOT NULL DEFAULT 0,
    planlanan_zaman      TIMESTAMPTZ,
    deneme_sayisi        INT NOT NULL DEFAULT 0,
    ilerleme_yuzde       INT NOT NULL DEFAULT 0 CHECK (ilerleme_yuzde BETWEEN 0 AND 100),
    sonuc                JSONB,
    hata                 TEXT,
    trace_id             TEXT,
    olusturulma          TIMESTAMPTZ NOT NULL DEFAULT now(),
    guncellenme          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_gorevler_durum ON motor.gorevler (durum);
CREATE INDEX idx_gorevler_tip ON motor.gorevler (tip);
CREATE INDEX idx_gorevler_planlanan_zaman ON motor.gorevler (planlanan_zaman)
    WHERE durum = 'BEKLIYOR';

CREATE TABLE motor.gorev_denemeleri (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gorev_id       UUID NOT NULL REFERENCES motor.gorevler (id),
    deneme_no      INT NOT NULL,
    baslangic      TIMESTAMPTZ NOT NULL DEFAULT now(),
    bitis          TIMESTAMPTZ,
    durum          TEXT NOT NULL,
    hata_mesaji    TEXT,
    stack_trace    TEXT,
    worker_kimlik  TEXT NOT NULL,
    UNIQUE (gorev_id, deneme_no)
);

CREATE INDEX idx_gorev_denemeleri_gorev_id ON motor.gorev_denemeleri (gorev_id);

CREATE TABLE motor.zamanlanmis_gorevler (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tip              TEXT NOT NULL REFERENCES motor.gorev_tanimlari (tip),
    cron_ifadesi     TEXT NOT NULL,
    payload          JSONB NOT NULL,
    aktif            BOOLEAN NOT NULL DEFAULT true,
    son_calisma      TIMESTAMPTZ,
    sonraki_calisma  TIMESTAMPTZ
);

CREATE INDEX idx_zamanlanmis_gorevler_aktif ON motor.zamanlanmis_gorevler (aktif, sonraki_calisma);

CREATE TABLE motor.giden_mesajlar (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gorev_id      UUID NOT NULL REFERENCES motor.gorevler (id),
    yayinlandi_mi BOOLEAN NOT NULL DEFAULT false,
    olusturulma   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deneme        INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_giden_mesajlar_yayinlanmamis ON motor.giden_mesajlar (olusturulma)
    WHERE yayinlandi_mi = false;

CREATE TABLE motor.olu_mektup_kutusu (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gorev_id              UUID NOT NULL REFERENCES motor.gorevler (id),
    son_hata              TEXT NOT NULL,
    giris_zamani          TIMESTAMPTZ NOT NULL DEFAULT now(),
    yeniden_gonderildi_mi BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_olu_mektup_kutusu_gorev_id ON motor.olu_mektup_kutusu (gorev_id);
