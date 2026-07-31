package com.gorevplatformu.motorapi.guvenlik;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motor.security")
public record MotorGuvenlikOzellikleri(String clientId, String clientSecret, Jwt jwt) {

    public record Jwt(String gizliAnahtar, long gecerlilikDakika) {
    }
}
