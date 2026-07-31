package com.gorevplatformu.motorcekirdek;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GorevTipi {

    String value();

    int maxDeneme() default 5;

    int timeoutSaniye() default 60;

    int oncelik() default 0;
}
