package com.gorevplatformu.motorspringstarter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "olu_mektup_kutusu", schema = "motor")
public class OluMektupKutusu {

    @Id
    private UUID id;

    @Column(name = "gorev_id")
    private UUID gorevId;

    @Column(name = "son_hata", nullable = false)
    private String sonHata;

    @Column(name = "ham_mesaj")
    private String hamMesaj;

    @Column(name = "giris_zamani", nullable = false)
    private Instant girisZamani;

    @Column(name = "yeniden_gonderildi_mi", nullable = false)
    private boolean yenidenGonderildiMi;

    protected OluMektupKutusu() {
    }

    public static OluMektupKutusu isHatasi(UUID gorevId, String sonHata) {
        OluMektupKutusu kayit = new OluMektupKutusu();
        kayit.id = UUID.randomUUID();
        kayit.gorevId = gorevId;
        kayit.sonHata = sonHata;
        kayit.girisZamani = Instant.now();
        kayit.yenidenGonderildiMi = false;
        return kayit;
    }

    public static OluMektupKutusu zehirliMesaj(String sonHata, String hamMesaj) {
        OluMektupKutusu kayit = new OluMektupKutusu();
        kayit.id = UUID.randomUUID();
        kayit.gorevId = null;
        kayit.sonHata = sonHata;
        kayit.hamMesaj = hamMesaj;
        kayit.girisZamani = Instant.now();
        kayit.yenidenGonderildiMi = false;
        return kayit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGorevId() {
        return gorevId;
    }

    public String getSonHata() {
        return sonHata;
    }

    public String getHamMesaj() {
        return hamMesaj;
    }

    public Instant getGirisZamani() {
        return girisZamani;
    }

    public boolean isYenidenGonderildiMi() {
        return yenidenGonderildiMi;
    }

    public void yenidenGonderildiOlarakIsaretle() {
        this.yenidenGonderildiMi = true;
    }
}
