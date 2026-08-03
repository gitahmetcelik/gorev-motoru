package com.gorevplatformu.motorspringstarter;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Duz {@code @EntityScan(basePackages = starterPaketi)} kullanimi, {@code EntityScanPackages}
 * bean'ini starter paketiyle sinirlar ve tuketici uygulamanin kendi entity paketini (normalde
 * {@code AutoConfigurationPackages}'tan gelen fallback) devre disi birakir. Bu registrar,
 * starter paketini tuketicinin kendi taban paketiyle birlikte kaydeder ki her iki taraf da
 * taransin.
 */
class MotorEntityTaramaKayitEdici implements ImportBeanDefinitionRegistrar {

    static final String STARTER_PAKETI = "com.gorevplatformu.motorspringstarter";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> paketler = new LinkedHashSet<>();
        paketler.add(STARTER_PAKETI);
        if (registry instanceof BeanFactory beanFactory && AutoConfigurationPackages.has(beanFactory)) {
            paketler.addAll(AutoConfigurationPackages.get(beanFactory));
        }
        EntityScanPackages.register(registry, paketler);
    }
}
