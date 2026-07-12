package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticação que opera em modo bridge (ADR 008).
 *
 * <p>Suporta dois fluxos:</p>
 * <ul>
 *   <li><b>Token legado via Cookie:</b> Valida tokens HMAC256 gerados pelo {@link TokenService}
 *       e popula o {@link SecurityContextHolder}. Tokens em cookies são sempre legados.</li>
 *   <li><b>Token Keycloak via Header Authorization:</b> NÃO processa — delega ao
 *       OAuth2 Resource Server configurado em {@link SecurityConfigurations}.</li>
 * </ul>
 *
 * <p>Quando ambos os mecanismos estão ativos, o cookie tem prioridade.
 * Se não houver cookie, o Bearer token no header é processado pelo Spring OAuth2.</p>
 *
 * @see SecurityConfigurations
 * @since ADR 008
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository repository) {
        this.tokenService = tokenService;
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. Tenta recuperar token legado do cookie
        var cookieToken = recuperarTokenDoCookie(request);

        if (cookieToken != null) {
            // 2. Token legado encontrado no cookie — processar via HMAC256
            processarTokenLegado(cookieToken);
        }
        // Se não houver cookie, NÃO processa o Authorization header aqui.
        // O OAuth2 Resource Server do Spring cuidará de tokens Bearer do Keycloak.

        // 3. Continua o fluxo da requisição
        filterChain.doFilter(request, response);
    }

    /**
     * Processa token legado HMAC256 do cookie e popula o SecurityContext.
     */
    private void processarTokenLegado(String tokenJWT) {
        String subject = null;
        String role = "ADMIN";
        String nome = "";
        String email = "";
        Long id = null;

        try {
            var decodedJWT = tokenService.verificarToken(tokenJWT);
            if (decodedJWT != null) {
                subject = decodedJWT.getSubject();
                var claimId = decodedJWT.getClaim("id");
                id = (claimId != null && !claimId.isNull()) ? claimId.asLong() : null;
                var claimRole = decodedJWT.getClaim("role");
                role = (claimRole != null && !claimRole.isNull()) ? claimRole.asString() : "ADMIN";
                var claimNome = decodedJWT.getClaim("nome");
                nome = (claimNome != null && !claimNome.isNull()) ? claimNome.asString() : "";
                var claimEmail = decodedJWT.getClaim("email");
                email = (claimEmail != null && !claimEmail.isNull()) ? claimEmail.asString() : "";
            } else {
                subject = tokenService.getSubject(tokenJWT);
            }
        } catch (Exception e) {
            try {
                subject = tokenService.getSubject(tokenJWT);
            } catch (Exception ex) {
                logger.debug("Token legado inválido no cookie: {}", ex.getMessage());
                return; // Token inválido — não autenticar
            }
        }

        if (subject != null) {
            Usuario usuario;
            if (id == null) {
                try {
                    var userDetails = repository.findByLogin(subject);
                    if (userDetails instanceof Usuario) {
                        usuario = (Usuario) userDetails;
                    } else {
                        usuario = new Usuario(subject, "", role, nome, email);
                    }
                } catch (Exception e) {
                    usuario = new Usuario(subject, "", role, nome, email);
                }
            } else {
                usuario = new Usuario(subject, "", role, nome, email);
                usuario.setId(id);
            }

            // Força a autenticação no contexto do Spring Security
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    /**
     * Recupera token legado do cookie HTTP (prioridade).
     * Como fallback para Swagger/testes locais, verifica o Header Authorization
     * caso o token possua o issuer legado ("API Consorcio").
     */
    private String recuperarTokenDoCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}