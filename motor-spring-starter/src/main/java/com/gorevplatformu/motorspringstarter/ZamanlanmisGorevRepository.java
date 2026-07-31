package com.gorevplatformu.motorspringstarter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ZamanlanmisGorevRepository extends JpaRepository<ZamanlanmisGorev, UUID> {

    List<ZamanlanmisGorev> findByAktifTrueAndSonrakiCalismaLessThanEqual(Instant simdi);
}
