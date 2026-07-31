package com.gorevplatformu.motorapi.ornek;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.gorevplatformu.motorspringstarter.CalisanGorevKayitDefteri;
import com.gorevplatformu.motorspringstarter.GorevRepository;
import com.gorevplatformu.motorspringstarter.OluMektupKutusuRepository;
import com.gorevplatformu.motorspringstarter.OluMektupKutusuServisi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Profile("demo")
public class DemoGorevBaslatici implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoGorevBaslatici.class);

    private final GorevGonderici gorevGonderici;
    private final CalisanGorevKayitDefteri calisanGorevKayitDefteri;
    private final OluMektupKutusuRepository oluMektupKutusuRepository;
    private final OluMektupKutusuServisi oluMektupKutusuServisi;
    private final GorevRepository gorevRepository;
    private final ScheduledExecutorService zamanlayici = Executors.newSingleThreadScheduledExecutor();

    public DemoGorevBaslatici(GorevGonderici gorevGonderici, CalisanGorevKayitDefteri calisanGorevKayitDefteri,
                               OluMektupKutusuRepository oluMektupKutusuRepository,
                               OluMektupKutusuServisi oluMektupKutusuServisi, GorevRepository gorevRepository) {
        this.gorevGonderici = gorevGonderici;
        this.calisanGorevKayitDefteri = calisanGorevKayitDefteri;
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
        this.oluMektupKutusuServisi = oluMektupKutusuServisi;
        this.gorevRepository = gorevRepository;
    }

    @Override
    public void run(String... args) {
        echoTesti();
        idempotencyTesti();
        isbirlikciIptalTesti();
        timeoutTesti();
        retryVeDlqTesti();
        oncelikTesti();
    }

    private void echoTesti() {
        UUID echoGorevId = gorevGonderici.gonder(
                "ornek.echo",
                new OrnekEchoHandler.Payload("Faz 2 kapi testi"),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), 0, null)
        );
        log.info("DEMO ECHO: {}", echoGorevId);
    }

    private void idempotencyTesti() {
        String anahtar = "demo-idempotency-" + UUID.randomUUID();
        UUID birinci = gorevGonderici.gonder("ornek.echo", new OrnekEchoHandler.Payload("ilk"),
                new GorevOpsiyonlari(anahtar, 0, null));
        UUID ikinci = gorevGonderici.gonder("ornek.echo", new OrnekEchoHandler.Payload("ikinci-ayni-anahtar"),
                new GorevOpsiyonlari(anahtar, 0, null));
        log.info("DEMO IDEMPOTENCY: birinci={} ikinci={} ayni-mi={}", birinci, ikinci, birinci.equals(ikinci));
    }

    private void isbirlikciIptalTesti() {
        UUID bekleGorevId = gorevGonderici.gonder(
                "ornek.bekle",
                new OrnekBekleHandler.Payload(10),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), 0, null)
        );
        log.info("DEMO ISBIRLIKCI IPTAL: {} gonderildi, calismaya baslamasi beklenip iptal istenecek",
                bekleGorevId);
        iptalIsteDeneyerek(bekleGorevId, 0);
    }

    private void iptalIsteDeneyerek(UUID bekleGorevId, int deneme) {
        if (deneme >= 20) {
            log.warn("DEMO ISBIRLIKCI IPTAL: {} 6sn icinde calisirken yakalanamadi, vazgeciliyor", bekleGorevId);
            return;
        }
        boolean bulundu = calisanGorevKayitDefteri.iptalIste(bekleGorevId);
        if (bulundu) {
            log.info("DEMO ISBIRLIKCI IPTAL: {} calisirken yakalandi ve iptal istendi (deneme {})",
                    bekleGorevId, deneme + 1);
            return;
        }
        zamanlayici.schedule(() -> iptalIsteDeneyerek(bekleGorevId, deneme + 1), 300, TimeUnit.MILLISECONDS);
    }

    private void timeoutTesti() {
        UUID zamanAsimiGorevId = gorevGonderici.gonder(
                "ornek.bekle",
                new OrnekBekleHandler.Payload(15),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), 0, null)
        );
        log.info("DEMO TIMEOUT: {} gonderildi (15sn bekleyecek, timeout 5sn, iptal edilmeyecek)",
                zamanAsimiGorevId);
    }

    private void retryVeDlqTesti() {
        UUID hataGorevId = gorevGonderici.gonder(
                "ornek.hata-ver",
                new OrnekHataVerHandler.Payload("kasitli-faz3-kapi-testi"),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), 0, null)
        );
        log.info("DEMO RETRY+DLQ: {} gonderildi, 3 deneme + backoff sonrasi DLQ'ya dusmesi bekleniyor", hataGorevId);

        zamanlayici.schedule(() -> dlqKontrolEt(hataGorevId, 0), 10, TimeUnit.SECONDS);
    }

    private void dlqKontrolEt(UUID hataGorevId, int deneme) {
        var kayit = oluMektupKutusuRepository.findFirstByGorevIdOrderByGirisZamaniDesc(hataGorevId);
        if (kayit.isEmpty()) {
            if (deneme >= 15) {
                log.warn("DEMO RETRY+DLQ: {} icin olu_mektup_kutusu kaydi bulunamadi (60sn icinde), vazgeciliyor",
                        hataGorevId);
                return;
            }
            zamanlayici.schedule(() -> dlqKontrolEt(hataGorevId, deneme + 1), 4, TimeUnit.SECONDS);
            return;
        }
        log.info("DEMO RETRY+DLQ: {} DLQ'ya dustu (kayit={}), simdi yeniden gonderiliyor",
                hataGorevId, kayit.get().getId());
        oluMektupKutusuServisi.yenidenGonder(kayit.get().getId());
        log.info("DEMO RETRY+DLQ: {} DLQ'dan yeniden gonderildi", hataGorevId);
    }

    private static final int ONCELIK_BACKLOG_ADEDI = 12;

    private void oncelikTesti() {
        List<UUID> normalGorevler = new ArrayList<>();
        for (int i = 0; i < ONCELIK_BACKLOG_ADEDI; i++) {
            normalGorevler.add(gorevGonderici.gonder("ornek.bekle", new OrnekBekleHandler.Payload(3),
                    new GorevOpsiyonlari(UUID.randomUUID().toString(), 0, null)));
        }
        UUID yuksekGorevId = gorevGonderici.gonder("ornek.bekle", new OrnekBekleHandler.Payload(1),
                new GorevOpsiyonlari(UUID.randomUUID().toString(), 10, null));
        log.info("DEMO ONCELIK: {} normal-oncelikli gorevle backlog olusturuldu, {} yuksek-oncelikli gorev "
                + "hemen ardindan gonderildi", ONCELIK_BACKLOG_ADEDI, yuksekGorevId);

        zamanlayici.schedule(() -> oncelikKontrolEt(yuksekGorevId, normalGorevler, 0), 6, TimeUnit.SECONDS);
    }

    private void oncelikKontrolEt(UUID yuksekGorevId, List<UUID> normalGorevler, int deneme) {
        GorevDurumu yuksekDurum = gorevRepository.findById(yuksekGorevId).orElseThrow().getDurum();
        long bitenNormalSayisi = normalGorevler.stream()
                .map(id -> gorevRepository.findById(id).orElseThrow().getDurum())
                .filter(durum -> durum == GorevDurumu.TAMAMLANDI)
                .count();
        log.info("DEMO ONCELIK: yuksek-oncelikli gorev durumu={}, {} normal-oncelikli gorevden {} tanesi tamamlandi "
                + "(hala kuyrukta bekleyenler var iken yuksek-oncelikli one gecmis olmali)",
                yuksekDurum, normalGorevler.size(), bitenNormalSayisi);
        if (yuksekDurum == GorevDurumu.TAMAMLANDI && bitenNormalSayisi < normalGorevler.size()) {
            log.info("DEMO ONCELIK: BASARILI — yuksek-oncelikli gorev, dolu normal kuyrugun onune gecti");
        } else if (deneme < 5) {
            zamanlayici.schedule(() -> oncelikKontrolEt(yuksekGorevId, normalGorevler, deneme + 1), 3,
                    TimeUnit.SECONDS);
        } else {
            log.warn("DEMO ONCELIK: yuksek-oncelikli gorev beklenen sekilde one gecmedi (durum={})", yuksekDurum);
        }
    }
}
