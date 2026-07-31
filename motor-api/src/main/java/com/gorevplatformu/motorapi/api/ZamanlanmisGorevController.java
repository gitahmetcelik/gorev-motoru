package com.gorevplatformu.motorapi.api;

import com.gorevplatformu.motorapi.api.dto.ZamanlanmisGorevCevabi;
import com.gorevplatformu.motorapi.api.dto.ZamanlanmisGorevOlusturmaIstegi;
import com.gorevplatformu.motorspringstarter.ZamanlanmisGorev;
import com.gorevplatformu.motorspringstarter.ZamanlanmisGorevServisi;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zamanlanmis-gorevler")
public class ZamanlanmisGorevController {

    private final ZamanlanmisGorevServisi zamanlanmisGorevServisi;

    public ZamanlanmisGorevController(ZamanlanmisGorevServisi zamanlanmisGorevServisi) {
        this.zamanlanmisGorevServisi = zamanlanmisGorevServisi;
    }

    @PostMapping
    public ResponseEntity<ZamanlanmisGorevCevabi> olustur(@Valid @RequestBody ZamanlanmisGorevOlusturmaIstegi istek) {
        ZamanlanmisGorev olusturulan = zamanlanmisGorevServisi.olustur(
                istek.tip(), istek.cronIfadesi(), istek.payload(), istek.aktif());
        return ResponseEntity.status(HttpStatus.CREATED).body(ZamanlanmisGorevCevabi.olustur(olusturulan));
    }
}
