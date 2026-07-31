package com.gorevplatformu.motorapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// motor-spring-starter artik gercek bir Spring Boot auto-configuration starter'i (bkz
// MotorOtomatikYapilandirmasi + META-INF/spring/...AutoConfiguration.imports) - burada ayrica
// scanBasePackages/@EnableJpaRepositories/@EntityScan ile onu "elle" dahil etmeye gerek yok,
// starter kendi paketini kendisi tariyor. Bu uygulama sadece kendi paketini (controller'lar,
// guvenlik, demo handler'lar) tarar.
@SpringBootApplication
public class GorevPlatformuUygulamasi {

    public static void main(String[] args) {
        SpringApplication.run(GorevPlatformuUygulamasi.class, args);
    }
}
