package com.gorevplatformu.motorcekirdek;

import java.util.UUID;

public interface GorevGonderici {

    UUID gonder(String tip, Object payload, GorevOpsiyonlari opsiyonlar);
}
