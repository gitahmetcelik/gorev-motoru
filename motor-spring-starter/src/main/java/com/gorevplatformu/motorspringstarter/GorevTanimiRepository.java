package com.gorevplatformu.motorspringstarter;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GorevTanimiRepository extends JpaRepository<GorevTanimi, String> {

    // existsById+save iki adimli kontrolun yerine: ayni gorev tipini ayni anda kaydeden
    // birden fazla instance (orn. api+worker, taze bir DB'de ayni anda acilirsa) PK
    // ihlaline carpiyordu — biri kazaniyor, digeri DataIntegrityViolationException
    // firlatiyor, bu da ApplicationStartedEvent listener'indan yayilip uygulamanin hic
    // acilmamasina yol aciyordu. Native upsert, yarisi DB seviyesinde atomik olarak cozer.
    @Modifying
    @Query(value = """
            INSERT INTO motor.gorev_tanimlari (tip, versiyon, kuyruk, varsayilan_oncelik, varsayilan_retry, timeout_sn, olusturulma)
            VALUES (:tip, 1, :kuyruk, :varsayilanOncelik, :varsayilanRetry, :timeoutSn, :olusturulma)
            ON CONFLICT (tip) DO NOTHING
            """, nativeQuery = true)
    void upsertYoksa(@Param("tip") String tip, @Param("kuyruk") String kuyruk,
                      @Param("varsayilanOncelik") Integer varsayilanOncelik,
                      @Param("varsayilanRetry") Integer varsayilanRetry,
                      @Param("timeoutSn") Integer timeoutSn, @Param("olusturulma") Instant olusturulma);
}
