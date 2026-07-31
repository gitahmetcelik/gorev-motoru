package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GorevRepository extends JpaRepository<Gorev, UUID> {

    Optional<Gorev> findByIdempotencyAnahtari(String idempotencyAnahtari);

    @Query("""
            SELECT g FROM Gorev g
            WHERE (:durum IS NULL OR g.durum = :durum)
              AND (:tip IS NULL OR g.tip = :tip)
            """)
    Page<Gorev> sayfala(@Param("durum") GorevDurumu durum, @Param("tip") String tip, Pageable sayfalama);

    @Query("SELECT g.id FROM Gorev g WHERE g.id IN :idler AND g.iptalIstendi = true")
    List<UUID> iptalIstenenIdleriBul(@Param("idler") Collection<UUID> idler);
}
