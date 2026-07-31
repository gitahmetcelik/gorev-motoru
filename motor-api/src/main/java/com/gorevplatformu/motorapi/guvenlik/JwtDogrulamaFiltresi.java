package com.gorevplatformu.motorapi.guvenlik;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtDogrulamaFiltresi extends OncePerRequestFilter {

    private final JwtServisi jwtServisi;

    public JwtDogrulamaFiltresi(JwtServisi jwtServisi) {
        this.jwtServisi = jwtServisi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse yanit, FilterChain zincir)
            throws ServletException, IOException {
        String baslik = istek.getHeader(HttpHeaders.AUTHORIZATION);
        if (baslik != null && baslik.startsWith("Bearer ")) {
            try {
                Jws<Claims> jws = jwtServisi.dogrula(baslik.substring(7));
                String clientId = jws.getPayload().getSubject();
                var authentication = new UsernamePasswordAuthenticationToken(
                        clientId, null, List.of(new SimpleGrantedAuthority("ROLE_GOREV_ISTEMCISI")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // gecersiz/suresi dolmus token: authentication set edilmez, asagida 401 doner
            }
        }
        zincir.doFilter(istek, yanit);
    }
}
