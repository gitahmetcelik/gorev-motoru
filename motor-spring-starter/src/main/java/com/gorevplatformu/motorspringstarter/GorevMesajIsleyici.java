package com.gorevplatformu.motorspringstarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevBaglamiImpl;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevIptalEdildiException;
import com.gorevplatformu.motorcekirdek.GorevTipi;
import com.gorevplatformu.motorcekirdek.YenidenDenemeGecikmeHesaplayici;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

/**
 * Bir gorev mesajinin is mantigini (durum gecisleri, deneme kaydi, handler calistirma,
 * retry/DLQ) barindirir. Faz4'ten itibaren dogrudan bir RabbitMQ listener DEGIL — hem
 * {@link GorevOncelikliTuketici} hem de (varsa) baska bir tetikleyici bu metodu cagirir,
 * boylece is mantigi tek yerde kalir.
 */
@Component
public class GorevMesajIsleyici {

    private static final Logger log = LoggerFactory.getLogger(GorevMesajIsleyici.class);

    private final GorevRepository gorevRepository;
    private final GorevDenemesiRepository gorevDenemesiRepository;
    private final GidenMesajRepository gidenMesajRepository;
    private final OluMektupKutusuRepository oluMektupKutusuRepository;
    private final GorevTipiKayitDefteri kayitDefteri;
    private final WorkerKimligi workerKimligi;
    private final GorevCalistirici gorevCalistirici;
    private final CalisanGorevKayitDefteri calisanGorevKayitDefteri;
    private final ObjectMapper objectMapper;
    private final GorevMetrikleri gorevMetrikleri;

    public GorevMesajIsleyici(GorevRepository gorevRepository, GorevDenemesiRepository gorevDenemesiRepository,
                              GidenMesajRepository gidenMesajRepository,
                              OluMektupKutusuRepository oluMektupKutusuRepository,
                              GorevTipiKayitDefteri kayitDefteri, WorkerKimligi workerKimligi,
                              GorevCalistirici gorevCalistirici,
                              CalisanGorevKayitDefteri calisanGorevKayitDefteri,
                              ObjectMapper objectMapper, GorevMetrikleri gorevMetrikleri) {
        this.gorevRepository = gorevRepository;
        this.gorevDenemesiRepository = gorevDenemesiRepository;
        this.gidenMesajRepository = gidenMesajRepository;
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
        this.kayitDefteri = kayitDefteri;
        this.workerKimligi = workerKimligi;
        this.gorevCalistirici = gorevCalistirici;
        this.calisanGorevKayitDefteri = calisanGorevKayitDefteri;
        this.objectMapper = objectMapper;
        this.gorevMetrikleri = gorevMetrikleri;
    }

    @Transactional
    public void isle(GorevMesaji mesaj) {
        // Trace id, mesaj isleme boyunca butun log satirlarinin (bu metod + handler) ayni id ile
        // korelasyona girebilmesi icin MDC'ye konuyor. GorevOncelikliTuketici push-model bir
        // @RabbitListener degil, senkron basicGet ile calisan ozel bir tuketici oldugu icin Spring
        // AMQP'nin otomatik trace-context yayilimi burada devreye girmiyor - elle tasiniyor.
        MDC.put("traceId", mesaj.traceId() != null ? mesaj.traceId() : "bilinmeyen");
        try {
            isleGercek(mesaj);
        } finally {
            MDC.remove("traceId");
        }
    }

    private void isleGercek(GorevMesaji mesaj) {
        Gorev gorev = gorevRepository.findById(mesaj.gorevId())
                .orElseThrow(() -> new IllegalStateException("Gorev bulunamadi: " + mesaj.gorevId()));

        // Handler guard (idempotency'nin 2. katmani): BEKLIYOR/KUYRUKTA disinda bir durumda
        // ise bu, RabbitMQ'nun ayni mesaji tekrar teslim etmesidir (ack kaybi, baglanti kopmasi vb.,
        // at-least-once teslimatta kacinilmaz) — gonderimdeki UNIQUE anahtar dedup'i bunu yakalayamaz
        // cunku ayni gorevId zaten var. Handler'i tekrar calistirmadan (yan etkiyi tekrarlamadan)
        // mesaji sessizce onayliyoruz.
        GorevDurumu mevcutDurum = gorev.getDurum();
        if (mevcutDurum != GorevDurumu.BEKLIYOR && mevcutDurum != GorevDurumu.KUYRUKTA) {
            log.info("Tekrar teslim edilen mesaj atlaniyor (idempotency guard): {} ({}), durum={}",
                    gorev.getId(), gorev.getTip(), mevcutDurum);
            return;
        }

        gorev.durumGecisYap(GorevDurumu.CALISIYOR);
        gorev.denemeSayisiniArtir();

        GorevDenemesi deneme = new GorevDenemesi(gorev.getId(), gorev.getDenemeSayisi(), workerKimligi.getKimlik());
        gorevDenemesiRepository.save(deneme);

        GorevHandler<?> handler = kayitDefteri.handlerBul(gorev.getTip());
        GorevTipi tanim = kayitDefteri.tanimBul(gorev.getTip());
        GorevBaglamiImpl baglam = new GorevBaglamiImpl();
        calisanGorevKayitDefteri.basladi(gorev.getId(), baglam);

        try {
            Object sonuc = calistir(handler, mesaj, baglam, Duration.ofSeconds(tanim.timeoutSaniye()));
            gorev.durumGecisYap(GorevDurumu.TAMAMLANDI);
            gorev.sonucGuncelle(sonuc == null ? null : objectMapper.writeValueAsString(sonuc));
            deneme.tamamlandi();
            gorevMetrikleri.gorevSonuclandi(gorev.getTip(), "basarili", Duration.between(deneme.getBaslangic(), deneme.getBitis()));
        } catch (GorevIptalEdildiException e) {
            log.info("Gorev iptal edildi: {} ({})", gorev.getId(), gorev.getTip());
            gorev.durumGecisYap(GorevDurumu.IPTAL_EDILDI);
            deneme.iptalEdildi();
            gorevMetrikleri.gorevSonuclandi(gorev.getTip(), "iptal", Duration.between(deneme.getBaslangic(), deneme.getBitis()));
        } catch (Exception e) {
            log.warn("Gorev basarisiz: {} ({}), deneme {}/{}", gorev.getId(), gorev.getTip(),
                    gorev.getDenemeSayisi(), tanim.maxDeneme(), e);
            String hataMesaji = e.getMessage();
            deneme.basarisiz(hataMesaji, izYigininiYaziyaCevir(e));
            gorev.hataGuncelle(hataMesaji);

            if (gorev.getDenemeSayisi() < tanim.maxDeneme()) {
                gorev.durumGecisYap(GorevDurumu.YENIDEN_DENENECEK);
                long gecikmeMs = YenidenDenemeGecikmeHesaplayici.hesapla(gorev.getDenemeSayisi());
                gidenMesajRepository.save(new GidenMesaj(gorev.getId(), (int) gecikmeMs));
                gorevMetrikleri.yenidenDenemeSayildi(gorev.getTip());
            } else {
                gorev.durumGecisYap(GorevDurumu.BASARISIZ);
                oluMektupKutusuRepository.save(OluMektupKutusu.isHatasi(gorev.getId(), hataMesaji));
                gorevMetrikleri.gorevSonuclandi(gorev.getTip(), "basarisiz", Duration.between(deneme.getBaslangic(), deneme.getBitis()));
            }
        } finally {
            calisanGorevKayitDefteri.bitti(gorev.getId());
        }

        gorevDenemesiRepository.save(deneme);
    }

    private <P> Object calistir(GorevHandler<P> handler, GorevMesaji mesaj, GorevBaglamiImpl baglam,
                                 Duration zamanAsimi) throws Exception {
        P payload = objectMapper.treeToValue(mesaj.payload(), handler.payloadTipi());
        return gorevCalistirici.calistir(handler, payload, baglam, zamanAsimi);
    }

    private String izYigininiYaziyaCevir(Exception e) {
        StringWriter yazici = new StringWriter();
        e.printStackTrace(new PrintWriter(yazici));
        return yazici.toString();
    }
}
