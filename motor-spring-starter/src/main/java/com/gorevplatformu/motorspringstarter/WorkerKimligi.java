package com.gorevplatformu.motorspringstarter;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

@Component
public class WorkerKimligi {

    private final String kimlik;

    public WorkerKimligi() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "bilinmeyen";
        }
        this.kimlik = host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getKimlik() {
        return kimlik;
    }
}
