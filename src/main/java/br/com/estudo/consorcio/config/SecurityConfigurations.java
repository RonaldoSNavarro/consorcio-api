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
 * <p>Integração OAuth2 Resource Server com validação JWT via JWKS (ADR 008, Lote 2).</p>
 * <p>Autenticação local legada (HMAC256) foi totalmente descontinuada e removida.</p>
 *
 * @since ADR 008
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfigurations {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurations.class);

    private final br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;
    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfigurations(
            br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter,
            KeycloakJwtConverter keycloakJwtConverter) {
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

                    // Permissões específicas
                    req.requestMatchers(HttpMethod.POST, "/api/contemplacoes/lances/{id}/integralizar").hasAnyAuthority("SCOPE_assembleia:execute");
                    req.requestMatchers(HttpMethod.POST, "/api/cotas/{id}/reembolsar").hasAnyAuthority("SCOPE_financeiro:write", "SCOPE_assembleia:execute");

                    // RBAC granular (Lote 2)
                    req.requestMatchers(HttpMethod.GET, "/api/compliance/**").hasAnyAuthority("SCOPE_compliance:read");
                    req.requestMatchers(HttpMethod.POST, "/api/compliance/**").hasAnyAuthority("SCOPE_compliance:screen");
                    req.requestMatchers(HttpMethod.PUT, "/api/compliance/**").hasAnyAuthority("SCOPE_compliance:screen");
                    req.requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("SCOPE_admin:full");
                    req.requestMatchers(HttpMethod.PUT, "/api/**").hasAuthority("SCOPE_admin:full");
                    req.requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("SCOPE_admin:full");

                    // Relatórios
                    req.requestMatchers(HttpMethod.GET, "/api/relatorios/**").hasAnyAuthority("SCOPE_admin:full", "SCOPE_financeiro:read");

                    // Leituras gerais
                    req.requestMatchers(HttpMethod.GET, "/api/**").authenticated(); // As regras serão mais finas via @PreAuthorize ou OwnershipGuard
                    
                    req.anyRequest().authenticated();
                })
                // OAuth2 Resource Server — valida tokens RS256 do Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                )
                .addFilterBefore(intrusionDetectionFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * JwtDecoder padrão. (Modo bridge removido no Lote 2).
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
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