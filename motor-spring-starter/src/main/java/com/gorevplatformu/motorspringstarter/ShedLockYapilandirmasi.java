package com.gorevplatformu.motorspringstarter;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Coklu worker instance'ta zamanlanmis-gorev tetikleyicisinin (bkz ZamanlanmisGorevCalistirici)
 * ayni anda sadece bir instance'ta calismasini garantiler. Tablo motor semasinda tutuluyor
 * (JPA entity'lerdeki gibi acikca sema-nitelikli), cunku datasource baglantisinin varsayilan
 * search_path'ine (public) guvenilemez.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(LockProvider.class)
@EnableSchedulerLock(defaultLockAtMostFor = "PT2M")
public class ShedLockYapilandirmasi {

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("motor.shedlock")
                        .usingDbTime()
                        .build());
    }
}
