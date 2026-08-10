-- Isbirlikci iptal sinyali: API'nin hangi worker'in gorevi calistirdigini bilmesine gerek kalmadan,
-- kontrol sinyalini DB uzerinden (tek dogruluk kaynagi) tasimasi icin.
ALTER TABLE motor.gorevler
    ADD COLUMN iptal_istendi BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_gorevler_iptal_istendi ON motor.gorevler (iptal_istendi) WHERE iptal_istendi = true;
