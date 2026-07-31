package com.gorevplatformu.motorapi.api.dto;

import com.gorevplatformu.motorspringstarter.GorevTanimi;

public record GorevTanimiCevabi(String tip, Integer versiyon, String kuyruk, Integer varsayilanOncelik,
                                 Integer varsayilanRetry, Integer timeoutSn) {

    public static GorevTanimiCevabi olustur(GorevTanimi tanim) {
        return new GorevTanimiCevabi(tanim.getTip(), tanim.getVersiyon(), tanim.getKuyruk(),
                tanim.getVarsayilanOncelik(), tanim.getVarsayilanRetry(), tanim.getTimeoutSn());
    }
}
