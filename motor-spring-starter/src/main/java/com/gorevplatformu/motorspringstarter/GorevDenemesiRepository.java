package com.gorevplatformu.motorspringstarter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GorevDenemesiRepository extends JpaRepository<GorevDenemesi, UUID> {

    List<GorevDenemesi> findByGorevIdOrderByBaslangicAsc(UUID gorevId);
}
