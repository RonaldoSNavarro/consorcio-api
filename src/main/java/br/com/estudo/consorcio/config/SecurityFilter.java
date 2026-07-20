package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import br.com.estudo.consorcio.domain.model.Perfil;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository repository) {
        this.tokenService = tokenService;
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. Pega o token do cabeçalho da requisição
        var tokenJWT = recuperarToken(request);

        // 2. Se tiver um token, valida e autentica o usuário para essa requisição
        if (tokenJWT != null) {
            String subject = null;
            String role = "ADMIN";
            List<GrantedAuthority> authorities = new ArrayList<>();
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
                    var claimAuthorities = decodedJWT.getClaim("authorities");
                    if (claimAuthorities != null && !claimAuthorities.isNull()) {
                        authorities = claimAuthorities.asList(String.class).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    }
                    // Garante ROLE_<perfil> nas authorities para compatibilidade com hasRole()
                    if (role != null && !role.isBlank()) {
                        String roleAuth = "ROLE_" + role;
                        if (authorities.stream().noneMatch(a -> a.getAuthority().equals(roleAuth))) {
                            authorities.add(new SimpleGrantedAuthority(roleAuth));
                        }
                    }
                    var claimNome = decodedJWT.getClaim("nome");
                    nome = (claimNome != null && !claimNome.isNull()) ? claimNome.asString() : "";
                    var claimEmail = decodedJWT.getClaim("email");
                    email = (claimEmail != null && !claimEmail.isNull()) ? claimEmail.asString() : "";
                } else {
                    subject = tokenService.getSubject(tokenJWT);
                }
            } catch (Exception e) {
                System.err.println("Erro ao decodificar JWT no SecurityFilter:");
                e.printStackTrace();
                try {
                    subject = tokenService.getSubject(tokenJWT);
                } catch (Exception ex) {
                    System.err.println("Erro ao obter subject do JWT no SecurityFilter (fallback):");
                    ex.printStackTrace();
                }
            }

            if (subject != null) {
                Usuario usuario = null;
                try {
                    var userDetails = repository.findByLogin(subject);
                    if (userDetails instanceof Usuario) {
                        usuario = (Usuario) userDetails;
                    }
                } catch (Exception e) {
                    // Ignore
                }
                
                if (usuario == null) {
                    Perfil mockPerfil = new Perfil(role);
                    usuario = new Usuario(subject, "", mockPerfil, nome, email);
                    if (id != null) {
                        usuario.setId(id);
                    }
                }

                // Força a autenticação no contexto do Spring Security
                var auths = !authorities.isEmpty() ? authorities : usuario.getAuthorities();
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, auths);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 3. Continua o fluxo da requisição (se não tiver token, vai bater no bloqueio 403 depois)
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // Fallback for tools/swagger that might still send it via Authorization header
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        
        return null;
    }
}