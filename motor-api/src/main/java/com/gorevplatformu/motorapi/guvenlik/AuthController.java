package com.gorevplatformu.motorapi.guvenlik;

import com.gorevplatformu.motorapi.guvenlik.dto.KimlikBilgisiIstegi;
import com.gorevplatformu.motorapi.guvenlik.dto.TokenCevabi;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MotorGuvenlikOzellikleri ozellikler;
    private final JwtServisi jwtServisi;

    public AuthController(MotorGuvenlikOzellikleri ozellikler, JwtServisi jwtServisi) {
        this.ozellikler = ozellikler;
        this.jwtServisi = jwtServisi;
    }

    @PostMapping("/token")
    @SecurityRequirements
    public ResponseEntity<TokenCevabi> tokenAl(@Valid @RequestBody KimlikBilgisiIstegi istek) {
        boolean gecerli = sabitZamandaEsitMi(ozellikler.clientId(), istek.clientId())
                && sabitZamandaEsitMi(ozellikler.clientSecret(), istek.clientSecret());
        if (!gecerli) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtServisi.tokenUret(istek.clientId());
        return ResponseEntity.ok(new TokenCevabi(token, "Bearer", jwtServisi.gecerlilikSaniye()));
    }

    private boolean sabitZamandaEsitMi(String beklenen, String gelen) {
        return MessageDigest.isEqual(beklenen.getBytes(StandardCharsets.UTF_8), gelen.getBytes(StandardCharsets.UTF_8));
    }
}
