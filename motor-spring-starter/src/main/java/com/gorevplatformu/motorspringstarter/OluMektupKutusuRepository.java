package com.gorevplatformu.motorspringstarter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OluMektupKutusuRepository extends JpaRepository<OluMektupKutusu, UUID> {

    Optional<OluMektupKutusu> findFirstByGorevIdOrderByGirisZamaniDesc(UUID gorevId);

    @Query("""
            SELECT o FROM OluMektupKutusu o
            WHERE (:yenidenGonderildiMi IS NULL OR o.yenidenGonderildiMi = :yenidenGonderildiMi)
            """)
    Page<OluMektupKutusu> sayfala(@Param("yenidenGonderildiMi") Boolean yenidenGonderildiMi, Pageable sayfalama);
}
