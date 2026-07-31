package com.gorevplatformu.motorcekirdek;

import java.time.Duration;

public class GorevZamanAsimiException extends RuntimeException {

    public GorevZamanAsimiException(Duration zamanAsimi) {
        super("Gorev zaman asimina ugradi: " + zamanAsimi);
    }
}
