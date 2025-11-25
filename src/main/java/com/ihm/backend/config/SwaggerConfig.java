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
                        .title("️ Plateforme de Vote Électronique - API")
                        .description("""
                                ## Documentation API Complète
                                
                                Cette API permet de gérer une plateforme de cours en ligne  avec les fonctionnalités suivantes :
                                
                                ### Réponses API
                                Toutes les réponses suivent un format JSON standard avec gestion d'erreurs appropriée.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe Technique")
                                .email("piodjiele@gmail.com")
                                .url("https://github.com/PIO-VIA/Civix.git"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description(" Serveur de Développement"),
                        new Server()
                                .url("https://civix-1-23wr.onrender.com")
                                .description(" Serveur de Production")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token d'authentification Bearer (électeur ou admin)")));
    }
}

