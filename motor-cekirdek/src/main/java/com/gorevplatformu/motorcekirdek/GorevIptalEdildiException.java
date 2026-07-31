package com.gorevplatformu.motorcekirdek;

public class GorevIptalEdildiException extends RuntimeException {

    public GorevIptalEdildiException() {
        super("Gorev isbirlikci iptal ile durduruldu");
    }
}
