package com.gorevplatformu.motorspringstarter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "zamanlanmis_gorevler", schema = "motor")
public class ZamanlanmisGorev {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String tip;

    @Column(name = "cron_ifadesi", nullable = false)
    private String cronIfadesi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private boolean aktif;

    @Column(name = "son_calisma")
    private Instant sonCalisma;

    @Column(name = "sonraki_calisma")
    private Instant sonrakiCalisma;

    protected ZamanlanmisGorev() {
    }

    public ZamanlanmisGorev(String tip, String cronIfadesi, String payload, boolean aktif,
                             Instant sonrakiCalisma) {
        this.id = UUID.randomUUID();
        this.tip = tip;
        this.cronIfadesi = cronIfadesi;
        this.payload = payload;
        this.aktif = aktif;
        this.sonrakiCalisma = sonrakiCalisma;
    }

    public UUID getId() {
        return id;
    }

    public String getTip() {
        return tip;
    }

    public String getCronIfadesi() {
        return cronIfadesi;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isAktif() {
        return aktif;
    }

    public Instant getSonCalisma() {
        return sonCalisma;
    }

    public Instant getSonrakiCalisma() {
        return sonrakiCalisma;
    }

    public void calistirildiOlarakIsaretle(Instant calismaZamani, Instant yeniSonrakiCalisma) {
        this.sonCalisma = calismaZamani;
        this.sonrakiCalisma = yeniSonrakiCalisma;
    }
}
