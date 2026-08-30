package br.com.estudo.consorcio.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    @DisplayName("Deve gerar configuração completa do OpenAPI 3.0 com SecurityScheme e Metadados")
    void deveConfigurarOpenApiCorretamente() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertEquals("Consórcio API — Documentação Oficial OpenAPI 3.0", openAPI.getInfo().getTitle());
        assertEquals("v1.0.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey(OpenApiConfig.SECURITY_SCHEME_NAME));
        assertEquals(3, openAPI.getServers().size());
    }
}