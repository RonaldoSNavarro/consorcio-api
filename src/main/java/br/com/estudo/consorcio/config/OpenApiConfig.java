package br.com.estudo.consorcio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI consorcioOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-key"))
                .info(new Info()
                        .title("🏢 Consórcio API")
                        .description("API REST para administração e gerenciamento do ciclo de vida de consórcios, implementando regras normatizadas pelo Banco Central do Brasil (BCB) e tratamento de inadimplência pro-rata die.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ronaldo Navarro")
                                .url("https://github.com/RonaldoSNavarro")));
    }

    // === INTERCEPTADOR QUE GARANTE A ORDEM DAS ABAS ===
    @Bean
    public OpenApiCustomizer sortTagsCustomizer() {
        return openApi -> {
            // A ordem exata da sua jornada
            List<String> ordemDesejada = List.of(
                    "Autenticação",
                    "Clientes",
                    "Grupos",
                    "Cotas",
                    "Parcelas",
                    "Assembleias",
                    "Contemplações"
            );

            // Se as tags já foram mapeadas pelo Springdoc, ele as ordena com base na nossa lista
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparing(tag -> {
                    int index = ordemDesejada.indexOf(tag.getName());
                    return index == -1 ? 999 : index; // Se achar uma tag nova, joga pro fim da lista
                }));
            }
        };
    }
}