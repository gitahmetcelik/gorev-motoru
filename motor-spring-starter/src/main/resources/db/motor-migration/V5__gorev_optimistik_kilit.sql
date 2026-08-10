-- Optimistik kilit (JPA @Version): API'nin iptal yazisi ile worker'in uzun surebilen
-- (tum handler calismasi boyunca acik kalan) transaction'inin ayni gorev satirini es
-- zamanli guncellemesi durumunda, sonradan commit eden digerini sessizce ezmesin diye.
ALTER TABLE motor.gorevler
    ADD COLUMN versiyon BIGINT NOT NULL DEFAULT 0;
