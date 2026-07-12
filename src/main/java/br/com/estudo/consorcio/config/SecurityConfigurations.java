package br.com.estudo.consorcio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central de segurança do sistema.
 *
 * <p>Suporta dois mecanismos de autenticação em modo bridge (ADR 008):</p>
 * <ul>
 *   <li><b>Keycloak (primário):</b> Tokens RS256 validados via JWKS endpoint do Keycloak</li>
 *   <li><b>Legado (bridge):</b> Tokens HMAC256 gerados pelo {@code TokenService} interno</li>
 * </ul>
 *
 * <p>O {@link SecurityFilter} atua como bridge, processando tokens legados ANTES
 * do Resource Server do Spring processar tokens Keycloak.</p>
 *
 * @since ADR 008
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfigurations {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurations.class);

    private final SecurityFilter securityFilter;
    private final br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;
    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfigurations(
            SecurityFilter securityFilter,
            br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter,
            KeycloakJwtConverter keycloakJwtConverter) {
        this.securityFilter = securityFilter;
        this.intrusionDetectionFilter = intrusionDetectionFilter;
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Rotas públicas
                    req.requestMatchers(HttpMethod.POST, "/api/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/api/login/logout").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/api/login/me").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/api/auth/keycloak-config").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();

                    // Permissões específicas para GESTOR e ADMIN
                    req.requestMatchers(HttpMethod.POST, "/api/contemplacoes/lances/{id}/integralizar").hasAnyRole("ADMIN", "GESTOR");
                    req.requestMatchers(HttpMethod.POST, "/api/cotas/{id}/reembolsar").hasAnyRole("ADMIN", "GESTOR");

                    // FC-04 FIX: RBAC granular — operações de escrita requerem ADMIN
                    req.requestMatchers(HttpMethod.GET, "/api/compliance/**").hasAnyRole("ADMIN", "COMPLIANCE");
                    req.requestMatchers(HttpMethod.POST, "/api/compliance/**").hasAnyRole("ADMIN", "COMPLIANCE");
                    req.requestMatchers(HttpMethod.PUT, "/api/compliance/**").hasAnyRole("ADMIN", "COMPLIANCE");
                    req.requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN");

                    // Relatórios BCB: Apenas ADMIN e AUDITOR
                    req.requestMatchers(HttpMethod.GET, "/api/relatorios/**").hasAnyRole("ADMIN", "AUDITOR");

                    // Leituras: ADMIN e AUDITOR
                    req.requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "AUDITOR", "CONSORCIADO");

                    // Qualquer outra rota requer autenticação
                    req.anyRequest().authenticated();
                })
                // OAuth2 Resource Server — valida tokens RS256 do Keycloak (ADR 008)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(customBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                )
                // Filtros customizados executam ANTES do Resource Server
                .addFilterBefore(securityFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .addFilterBefore(intrusionDetectionFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * JwtDecoder resiliente: tenta conectar ao Keycloak; se indisponível,
     * cria um decoder que sempre falha (forçando o uso do token legado via bridge).
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        try {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        } catch (Exception e) {
            logger.warn("Keycloak indisponível em {}. Modo bridge ativo (somente tokens legados).", issuerUri);
            return token -> { throw new org.springframework.security.oauth2.jwt.BadJwtException(
                    "Keycloak indisponível. Use autenticação legada via cookie."); };
        }
    }

    private BearerTokenResolver customBearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> {
            String token = defaultResolver.resolve(request);
            if (token == null) return null;

            // Se for um token de teste sem formato JWT
            if (!token.contains(".")) {
                return null;
            }

            try {
                com.auth0.jwt.interfaces.DecodedJWT jwt = com.auth0.jwt.JWT.decode(token);
                if ("API Consorcio".equals(jwt.getIssuer())) {
                    return null; // Oculta tokens legados do filtro OAuth2
                }
            } catch (Exception e) {
                // Ignore decoding errors
            }
            return token;
        };
    }

    @Value("${api.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174,http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



    // Define qual é o algoritmo de criptografia (Hash) que usaremos nas senhas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}