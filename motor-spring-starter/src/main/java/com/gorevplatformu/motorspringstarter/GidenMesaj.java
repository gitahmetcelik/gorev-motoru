package com.gorevplatformu.motorspringstarter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "giden_mesajlar", schema = "motor")
public class GidenMesaj {

    @Id
    private UUID id;

    @Column(name = "gorev_id", nullable = false)
    private UUID gorevId;

    @Column(name = "yayinlandi_mi", nullable = false)
    private boolean yayinlandiMi;

    @Column(nullable = false)
    private Instant olusturulma;

    @Column(nullable = false)
    private Integer deneme;

    @Column(name = "gecikme_ms", nullable = false)
    private Integer gecikmeMs;

    protected GidenMesaj() {
    }

    public GidenMesaj(UUID gorevId) {
        this(gorevId, 0);
    }

    public GidenMesaj(UUID gorevId, int gecikmeMs) {
        this.id = UUID.randomUUID();
        this.gorevId = gorevId;
        this.yayinlandiMi = false;
        this.olusturulma = Instant.now();
        this.deneme = 0;
        this.gecikmeMs = gecikmeMs;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGorevId() {
        return gorevId;
    }

    public boolean isYayinlandiMi() {
        return yayinlandiMi;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    public Integer getDeneme() {
        return deneme;
    }

    public Integer getGecikmeMs() {
        return gecikmeMs;
    }

    public void yayinlandiOlarakIsaretle() {
        this.yayinlandiMi = true;
    }
}
