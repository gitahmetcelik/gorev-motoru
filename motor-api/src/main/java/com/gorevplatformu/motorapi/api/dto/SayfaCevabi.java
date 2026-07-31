package com.gorevplatformu.motorapi.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record SayfaCevabi<T>(List<T> icerik, int sayfaNo, int boyut, long toplamEleman, int toplamSayfa) {

    public static <T> SayfaCevabi<T> olustur(Page<T> sayfa) {
        return new SayfaCevabi<>(sayfa.getContent(), sayfa.getNumber(), sayfa.getSize(),
                sayfa.getTotalElements(), sayfa.getTotalPages());
    }
}
