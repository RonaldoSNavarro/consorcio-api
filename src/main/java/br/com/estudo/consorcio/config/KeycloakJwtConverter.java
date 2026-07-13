package br.com.estudo.consorcio.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converte tokens JWT emitidos pelo Keycloak para {@link AbstractAuthenticationToken}
 * do Spring Security, extraindo as realm roles do claim {@code realm_access.roles}.
 *
 * <p>Mapeia cada role do Keycloak para múltiplas {@link SimpleGrantedAuthority} granulares
 * com o prefixo {@code SCOPE_}, de acordo com a regra de negócios (ex: ADMIN -> SCOPE_admin:full).</p>
 *
 * @see SecurityConfigurations
 * @since ADR 008 — Migração OAuth2/OIDC via Keycloak
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Extrai as realm roles do claim {@code realm_access.roles} do JWT do Keycloak.
     *
     * @param jwt token JWT decodificado
     * @return coleção de {@link GrantedAuthority} com prefixo {@code SCOPE_}
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .flatMap(role -> mapRoleToScopes(role).stream())
                .collect(Collectors.toList());
    }

    /**
     * Converte roles legadas de alto nível para permissões granulares (Scopes OAuth2).
     */
    private List<GrantedAuthority> mapRoleToScopes(String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN" -> List.of(
                    new SimpleGrantedAuthority("SCOPE_admin:full"),
                    new SimpleGrantedAuthority("SCOPE_cliente:read"),
                    new SimpleGrantedAuthority("SCOPE_cliente:write"),
                    new SimpleGrantedAuthority("SCOPE_cota:read"),
                    new SimpleGrantedAuthority("SCOPE_cota:write"),
                    new SimpleGrantedAuthority("SCOPE_grupo:read"),
                    new SimpleGrantedAuthority("SCOPE_grupo:write"),
                    new SimpleGrantedAuthority("SCOPE_assembleia:read"),
                    new SimpleGrantedAuthority("SCOPE_assembleia:execute"),
                    new SimpleGrantedAuthority("SCOPE_financeiro:read"),
                    new SimpleGrantedAuthority("SCOPE_financeiro:write")
            );
            case "COMPLIANCE" -> List.of(
                    new SimpleGrantedAuthority("SCOPE_compliance:read"),
                    new SimpleGrantedAuthority("SCOPE_compliance:screen"),
                    new SimpleGrantedAuthority("SCOPE_cliente:read")
            );
            case "GESTOR" -> List.of(
                    new SimpleGrantedAuthority("SCOPE_cota:read"),
                    new SimpleGrantedAuthority("SCOPE_cota:write"),
                    new SimpleGrantedAuthority("SCOPE_grupo:read"),
                    new SimpleGrantedAuthority("SCOPE_grupo:write"),
                    new SimpleGrantedAuthority("SCOPE_assembleia:read"),
                    new SimpleGrantedAuthority("SCOPE_assembleia:execute")
            );
            case "AUDITOR" -> List.of(
                    new SimpleGrantedAuthority("SCOPE_cliente:read"),
                    new SimpleGrantedAuthority("SCOPE_cota:read"),
                    new SimpleGrantedAuthority("SCOPE_grupo:read"),
                    new SimpleGrantedAuthority("SCOPE_assembleia:read"),
                    new SimpleGrantedAuthority("SCOPE_financeiro:read"),
                    new SimpleGrantedAuthority("SCOPE_compliance:read")
            );
            case "CONSORCIADO" -> List.of(
                    new SimpleGrantedAuthority("SCOPE_cota:read"),
                    new SimpleGrantedAuthority("SCOPE_cliente:read")
            );
            default -> List.of(new SimpleGrantedAuthority("ROLE_" + role)); // Fallback para role nativa
        };
    }
}
