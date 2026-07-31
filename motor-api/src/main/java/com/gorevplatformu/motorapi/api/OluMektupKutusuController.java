package com.gorevplatformu.motorapi.api;

import com.gorevplatformu.motorapi.api.dto.OluMektupKutusuCevabi;
import com.gorevplatformu.motorapi.api.dto.SayfaCevabi;
import com.gorevplatformu.motorspringstarter.OluMektupKutusuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/olu-mektup-kutusu")
public class OluMektupKutusuController {

    private final OluMektupKutusuRepository oluMektupKutusuRepository;

    public OluMektupKutusuController(OluMektupKutusuRepository oluMektupKutusuRepository) {
        this.oluMektupKutusuRepository = oluMektupKutusuRepository;
    }

    @GetMapping
    public SayfaCevabi<OluMektupKutusuCevabi> listele(
            @RequestParam(required = false) Boolean yenidenGonderildiMi,
            @PageableDefault(size = 20, sort = "girisZamani", direction = Sort.Direction.DESC) Pageable sayfalama) {
        Page<OluMektupKutusuCevabi> sayfa = oluMektupKutusuRepository.sayfala(yenidenGonderildiMi, sayfalama)
                .map(OluMektupKutusuCevabi::olustur);
        return SayfaCevabi.olustur(sayfa);
    }
}
