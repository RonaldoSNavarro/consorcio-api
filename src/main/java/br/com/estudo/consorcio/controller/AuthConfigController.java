package br.com.estudo.consorcio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller público que expõe a configuração do Keycloak para o frontend.
 *
 * <p>Permite que o frontend SPA descubra dinamicamente as URLs do Keycloak
 * sem hardcoding, facilitando deploy em diferentes ambientes.</p>
 *
 * @since ADR 008 — Migração OAuth2/OIDC via Keycloak
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Configuração de Autenticação", description = "Endpoints de configuração OAuth2/OIDC")
public class AuthConfigController {

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm:consorcio}")
    private String keycloakRealm;

    @Value("${keycloak.frontend-client-id:consorcio-frontend}")
    private String keycloakFrontendClientId;

    /**
     * Retorna a configuração do Keycloak para o frontend inicializar o adapter OIDC.
     *
     * @return mapa com url, realm e clientId
     */
    @GetMapping("/keycloak-config")
    @Operation(summary = "Retorna configuração do Keycloak para o frontend SPA")
    public ResponseEntity<Map<String, String>> obterKeycloakConfig() {
        return ResponseEntity.ok(Map.of(
                "url", keycloakUrl,
                "realm", keycloakRealm,
                "clientId", keycloakFrontendClientId
        ));
    }
}
