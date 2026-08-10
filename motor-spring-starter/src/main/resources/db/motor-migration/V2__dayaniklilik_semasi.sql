ALTER TABLE motor.giden_mesajlar
    ADD COLUMN gecikme_ms INT NOT NULL DEFAULT 0;

ALTER TABLE motor.olu_mektup_kutusu
    ALTER COLUMN gorev_id DROP NOT NULL;

ALTER TABLE motor.olu_mektup_kutusu
    ADD COLUMN ham_mesaj TEXT;
