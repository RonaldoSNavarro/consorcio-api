package br.com.estudo.consorcio.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
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
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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

    // Ensina o Spring a injetar o Gerenciador de Autenticação nos Controllers
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Define qual é o algoritmo de criptografia (Hash) que usaremos nas senhas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}