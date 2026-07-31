package com.gorevplatformu.motorspringstarter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GidenMesajRepository extends JpaRepository<GidenMesaj, UUID> {

    List<GidenMesaj> findByYayinlandiMiFalseOrderByOlusturulmaAsc();
}
