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
 * <p>Mapeia cada role do Keycloak para uma {@link SimpleGrantedAuthority} com
 * o prefixo {@code ROLE_}, mantendo compatibilidade com as anotações
 * {@code @PreAuthorize("hasRole(...)")} existentes no sistema.</p>
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
     * @return coleção de {@link GrantedAuthority} com prefixo {@code ROLE_}
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
