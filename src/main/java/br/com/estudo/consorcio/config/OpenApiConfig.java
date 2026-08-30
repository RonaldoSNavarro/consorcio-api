package br.com.estudo.consorcio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "cookieAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Consórcio API — Documentação Oficial OpenAPI 3.0")
                        .description("API corporativa de alta performance para administração de consórcios segundo as normativas do Banco Central do Brasil (BACEN) e Lei 11.795/2008.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Arquitetura Consórcio")
                                .email("arquitetura@consorcio.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Ambiente Local de Desenvolvimento"),
                        new Server().url("https://api-staging.consorcio.com.br").description("Ambiente de Staging / Homologação"),
                        new Server().url("https://api.consorcio.com.br").description("Ambiente de Produção")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("jwt-token")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .description("Token JWT seguro transmitido via cookie HttpOnly (SameSite=Strict)")));
    }
}