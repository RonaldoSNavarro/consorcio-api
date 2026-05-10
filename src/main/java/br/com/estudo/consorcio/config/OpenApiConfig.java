package br.com.estudo.consorcio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI consorcioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏢 Consórcio API")
                        .description("API REST para administração e gerenciamento do ciclo de vida de consórcios, implementando regras normatizadas pelo Banco Central do Brasil (BCB) e tratamento de inadimplência pro-rata die.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ronaldo Navarro")
                                .url("https://github.com/RonaldoSNavarro")));
    }
}