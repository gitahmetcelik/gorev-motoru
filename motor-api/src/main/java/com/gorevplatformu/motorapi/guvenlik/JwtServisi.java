package com.gorevplatformu.motorapi.guvenlik;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtServisi {

    private final MotorGuvenlikOzellikleri ozellikler;
    private final SecretKey anahtar;

    public JwtServisi(MotorGuvenlikOzellikleri ozellikler) {
        this.ozellikler = ozellikler;
        this.anahtar = Keys.hmacShaKeyFor(ozellikler.jwt().gizliAnahtar().getBytes(StandardCharsets.UTF_8));
    }

    public String tokenUret(String clientId) {
        Instant simdi = Instant.now();
        return Jwts.builder()
                .subject(clientId)
                .issuedAt(Date.from(simdi))
                .expiration(Date.from(simdi.plus(ozellikler.jwt().gecerlilikDakika(), ChronoUnit.MINUTES)))
                .signWith(anahtar, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> dogrula(String token) {
        return Jwts.parser().verifyWith(anahtar).build().parseSignedClaims(token);
    }

    public long gecerlilikSaniye() {
        return ozellikler.jwt().gecerlilikDakika() * 60;
    }
}
