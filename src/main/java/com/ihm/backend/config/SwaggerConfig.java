package com.ihm.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                    .title("XXCM1 : Plateforme de cours Électronique - API")
                    .description("""
                            ## Documentation API Complète

                            API permettant de gérer une plateforme de cours en ligne,
                            incluant gestion des utilisateurs, cours, contenu, paiements, etc.
                            """)
                    .version("1.0.0")
                    .contact(new Contact()
                            .name("Équipe Technique")
                            .email("azangueleonel9@gmail.com")
                            .url("https://github.com/Prrojet-XCCM1/XCCM1-BACKEND"))
                    .license(new License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                    new Server()
                            .url("http://localhost:8080")
                            .description("Serveur de Développement"),
                    new Server()
                            .url("https://civix-1-23wr.onrender.com")
                            .description("Serveur de Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                    .addSecuritySchemes("bearerAuth",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description(" Fournir un token JWT valide")));
    }
}
