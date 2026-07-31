package com.gorevplatformu.motorapi;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AcikApiYapilandirmasi {

    @Bean
    public OpenAPI gorevPlatformuOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Gorev Platformu API").version("v1")
                        .description("Gorev/is kuyrugu motoru REST API'si"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
