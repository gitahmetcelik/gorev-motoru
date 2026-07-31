package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevDurumGecisi;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gorevler", schema = "motor")
public class Gorev {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String tip;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GorevDurumu durum;

    @Column(name = "idempotency_anahtari", nullable = false, unique = true)
    private String idempotencyAnahtari;

    @Column(nullable = false)
    private Integer oncelik;

    @Column(name = "planlanan_zaman")
    private Instant planlananZaman;

    @Column(name = "deneme_sayisi", nullable = false)
    private Integer denemeSayisi;

    @Column(name = "ilerleme_yuzde", nullable = false)
    private Integer ilerlemeYuzde;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String sonuc;

    private String hata;

    @Column(name = "trace_id")
    private String traceId;

    @Column(nullable = false)
    private Instant olusturulma;

    @Column(nullable = false)
    private Instant guncellenme;

    @Column(name = "iptal_istendi", nullable = false)
    private boolean iptalIstendi;

    // Worker'in isle() metodu, handler calisirken tum sure boyunca ayni transaction'i acik
    // tutuyor (Faz2'den beri); bu esnada API'nin ayri bir transaction'da yaptigi bir yazi (orn.
    // iptal) versiyon kontrolu olmadan sessizce ezilebilirdi - hangisi SON commit ederse o kazanirdi,
    // mantiksal olarak yanlis olsa bile. @Version, gec commit eden tarafin
    // ObjectOptimisticLockingFailureException almasini saglar (bkz GorevOncelikliTuketici,
    // GorevYonetimServisi - ikisi de bu istisnayi bilerek/yumusakca ele aliyor).
    @Version
    @Column(nullable = false)
    private long versiyon;

    protected Gorev() {
    }

    public Gorev(String tip, String payload, String idempotencyAnahtari, Integer oncelik,
                 Instant planlananZaman, String traceId) {
        Instant simdi = Instant.now();
        this.id = UUID.randomUUID();
        this.tip = tip;
        this.payload = payload;
        this.durum = GorevDurumu.BEKLIYOR;
        this.idempotencyAnahtari = idempotencyAnahtari;
        this.oncelik = oncelik;
        this.planlananZaman = planlananZaman;
        this.denemeSayisi = 0;
        this.ilerlemeYuzde = 0;
        this.traceId = traceId;
        this.olusturulma = simdi;
        this.guncellenme = simdi;
        this.iptalIstendi = false;
    }

    public UUID getId() {
        return id;
    }

    public String getTip() {
        return tip;
    }

    public String getPayload() {
        return payload;
    }

    public GorevDurumu getDurum() {
        return durum;
    }

    public String getIdempotencyAnahtari() {
        return idempotencyAnahtari;
    }

    public Integer getOncelik() {
        return oncelik;
    }

    public Instant getPlanlananZaman() {
        return planlananZaman;
    }

    public Integer getDenemeSayisi() {
        return denemeSayisi;
    }

    public Integer getIlerlemeYuzde() {
        return ilerlemeYuzde;
    }

    public String getSonuc() {
        return sonuc;
    }

    public String getHata() {
        return hata;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    public Instant getGuncellenme() {
        return guncellenme;
    }

    public boolean isIptalIstendiMi() {
        return iptalIstendi;
    }

    public void iptalIstendiOlarakIsaretle() {
        this.iptalIstendi = true;
        this.guncellenme = Instant.now();
    }

    public void durumGecisYap(GorevDurumu hedef) {
        GorevDurumGecisi.dogrula(this.durum, hedef);
        this.durum = hedef;
        this.guncellenme = Instant.now();
    }

    public void denemeSayisiniArtir() {
        this.denemeSayisi = this.denemeSayisi + 1;
        this.guncellenme = Instant.now();
    }

    public void sonucGuncelle(String sonuc) {
        this.sonuc = sonuc;
        this.guncellenme = Instant.now();
    }

    public void hataGuncelle(String hata) {
        this.hata = hata;
        this.guncellenme = Instant.now();
    }
}
