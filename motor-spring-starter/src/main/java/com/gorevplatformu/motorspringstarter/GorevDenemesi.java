package com.gorevplatformu.motorspringstarter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gorev_denemeleri", schema = "motor")
public class GorevDenemesi {

    @Id
    private UUID id;

    @Column(name = "gorev_id", nullable = false)
    private UUID gorevId;

    @Column(name = "deneme_no", nullable = false)
    private Integer denemeNo;

    @Column(nullable = false)
    private Instant baslangic;

    private Instant bitis;

    @Column(nullable = false)
    private String durum;

    @Column(name = "hata_mesaji")
    private String hataMesaji;

    @Column(name = "stack_trace")
    private String stackTrace;

    @Column(name = "worker_kimlik", nullable = false)
    private String workerKimlik;

    protected GorevDenemesi() {
    }

    public GorevDenemesi(UUID gorevId, Integer denemeNo, String workerKimlik) {
        this.id = UUID.randomUUID();
        this.gorevId = gorevId;
        this.denemeNo = denemeNo;
        this.baslangic = Instant.now();
        this.durum = "CALISIYOR";
        this.workerKimlik = workerKimlik;
    }

    public void tamamlandi() {
        this.durum = "TAMAMLANDI";
        this.bitis = Instant.now();
    }

    public void iptalEdildi() {
        this.durum = "IPTAL_EDILDI";
        this.bitis = Instant.now();
        this.hataMesaji = "Iptal edildi";
    }

    public void basarisiz(String hataMesaji, String stackTrace) {
        this.durum = "BASARISIZ";
        this.bitis = Instant.now();
        this.hataMesaji = hataMesaji;
        this.stackTrace = stackTrace;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGorevId() {
        return gorevId;
    }

    public Integer getDenemeNo() {
        return denemeNo;
    }

    public String getDurum() {
        return durum;
    }

    public Instant getBaslangic() {
        return baslangic;
    }

    public Instant getBitis() {
        return bitis;
    }

    public String getHataMesaji() {
        return hataMesaji;
    }

    public String getWorkerKimlik() {
        return workerKimlik;
    }
}
