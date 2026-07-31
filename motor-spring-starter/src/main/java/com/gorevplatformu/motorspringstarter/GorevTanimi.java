package com.gorevplatformu.motorspringstarter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "gorev_tanimlari", schema = "motor")
public class GorevTanimi {

    @Id
    private String tip;

    @Column(nullable = false)
    private Integer versiyon;

    @Column(nullable = false)
    private String kuyruk;

    @Column(name = "varsayilan_oncelik", nullable = false)
    private Integer varsayilanOncelik;

    @Column(name = "varsayilan_retry", nullable = false)
    private Integer varsayilanRetry;

    @Column(name = "timeout_sn", nullable = false)
    private Integer timeoutSn;

    @Column(nullable = false)
    private Instant olusturulma;

    protected GorevTanimi() {
    }

    public GorevTanimi(String tip, String kuyruk, Integer varsayilanOncelik, Integer varsayilanRetry,
                        Integer timeoutSn) {
        this.tip = tip;
        this.versiyon = 1;
        this.kuyruk = kuyruk;
        this.varsayilanOncelik = varsayilanOncelik;
        this.varsayilanRetry = varsayilanRetry;
        this.timeoutSn = timeoutSn;
        this.olusturulma = Instant.now();
    }

    public String getTip() {
        return tip;
    }

    public Integer getVersiyon() {
        return versiyon;
    }

    public String getKuyruk() {
        return kuyruk;
    }

    public Integer getVarsayilanOncelik() {
        return varsayilanOncelik;
    }

    public Integer getVarsayilanRetry() {
        return varsayilanRetry;
    }

    public Integer getTimeoutSn() {
        return timeoutSn;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }
}
