package com.gorevplatformu.motorapi.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorapi.api.dto.GorevCevabi;
import com.gorevplatformu.motorapi.api.dto.GorevDenemesiCevabi;
import com.gorevplatformu.motorapi.api.dto.GorevOlusturmaIstegi;
import com.gorevplatformu.motorapi.api.dto.SayfaCevabi;
import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.gorevplatformu.motorspringstarter.Gorev;
import com.gorevplatformu.motorspringstarter.GorevBulunamadiException;
import com.gorevplatformu.motorspringstarter.GorevDenemesiRepository;
import com.gorevplatformu.motorspringstarter.GorevRepository;
import com.gorevplatformu.motorspringstarter.GorevTipiKayitDefteri;
import com.gorevplatformu.motorspringstarter.GorevYonetimServisi;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/gorevler")
public class GorevController {

    private final GorevGonderici gorevGonderici;
    private final GorevRepository gorevRepository;
    private final GorevDenemesiRepository gorevDenemesiRepository;
    private final GorevTipiKayitDefteri kayitDefteri;
    private final GorevYonetimServisi gorevYonetimServisi;
    private final ObjectMapper objectMapper;

    public GorevController(GorevGonderici gorevGonderici, GorevRepository gorevRepository,
                            GorevDenemesiRepository gorevDenemesiRepository, GorevTipiKayitDefteri kayitDefteri,
                            GorevYonetimServisi gorevYonetimServisi, ObjectMapper objectMapper) {
        this.gorevGonderici = gorevGonderici;
        this.gorevRepository = gorevRepository;
        this.gorevDenemesiRepository = gorevDenemesiRepository;
        this.kayitDefteri = kayitDefteri;
        this.gorevYonetimServisi = gorevYonetimServisi;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<GorevCevabi> olustur(@Valid @RequestBody GorevOlusturmaIstegi istek) {
        kayitDefteri.tanimBul(istek.tip()); // bilinmeyen tip -> IllegalArgumentException -> 400
        String idempotencyAnahtari = (istek.idempotencyAnahtari() == null || istek.idempotencyAnahtari().isBlank())
                ? UUID.randomUUID().toString()
                : istek.idempotencyAnahtari();
        UUID id = gorevGonderici.gonder(istek.tip(), istek.payload(),
                new GorevOpsiyonlari(idempotencyAnahtari, istek.oncelik(), istek.planlananZaman()));
        Gorev gorev = gorevRepository.findById(id).orElseThrow();
        return ResponseEntity.created(URI.create("/gorevler/" + id))
                .body(GorevCevabi.olustur(gorev, objectMapper));
    }

    @GetMapping("/{id}")
    public GorevCevabi getir(@PathVariable UUID id) {
        Gorev gorev = gorevRepository.findById(id).orElseThrow(() -> new GorevBulunamadiException(id));
        return GorevCevabi.olustur(gorev, objectMapper);
    }

    @GetMapping("/{id}/denemeler")
    public List<GorevDenemesiCevabi> denemeleriListele(@PathVariable UUID id) {
        if (!gorevRepository.existsById(id)) {
            throw new GorevBulunamadiException(id);
        }
        return gorevDenemesiRepository.findByGorevIdOrderByBaslangicAsc(id).stream()
                .map(GorevDenemesiCevabi::olustur)
                .toList();
    }

    @GetMapping
    public SayfaCevabi<GorevCevabi> listele(
            @RequestParam(required = false) GorevDurumu durum,
            @RequestParam(required = false) String tip,
            @PageableDefault(size = 20, sort = "olusturulma", direction = Sort.Direction.DESC) Pageable sayfalama) {
        Page<Gorev> sayfa = gorevRepository.sayfala(durum, tip, sayfalama);
        return SayfaCevabi.olustur(sayfa.map(gorev -> GorevCevabi.olustur(gorev, objectMapper)));
    }

    @PostMapping("/{id}/iptal")
    public ResponseEntity<Void> iptalEt(@PathVariable UUID id) {
        // Gorev tam bu sirada worker tarafindan da guncelleniyor olabilir (isle() tum handler
        // calismasi boyunca acik kalan uzun bir transaction). Versiyon uyusmazligi olursa,
        // guncel duruma gore bir kez daha deniyoruz - orn. gorev bu arada TAMAMLANDI'ya
        // gecmisse ikinci deneme dogru sekilde 409 (iptal edilemez) doner.
        try {
            gorevYonetimServisi.iptalIste(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            gorevYonetimServisi.iptalIste(id);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/yeniden-dene")
    public ResponseEntity<Void> yenidenDeneEt(@PathVariable UUID id) {
        gorevYonetimServisi.yenidenDene(id);
        return ResponseEntity.accepted().build();
    }
}
