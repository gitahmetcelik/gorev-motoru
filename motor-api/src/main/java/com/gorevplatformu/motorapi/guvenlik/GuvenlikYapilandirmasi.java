package com.gorevplatformu.motorapi.guvenlik;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(MotorGuvenlikOzellikleri.class)
public class GuvenlikYapilandirmasi {

    @Bean
    public JwtDogrulamaFiltresi jwtDogrulamaFiltresi(JwtServisi jwtServisi) {
        return new JwtDogrulamaFiltresi(jwtServisi);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDogrulamaFiltresi jwtDogrulamaFiltresi)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/token", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                            "/actuator/health", "/actuator/info", "/actuator/prometheus",
                            "/dashboard/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtDogrulamaFiltresi, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
