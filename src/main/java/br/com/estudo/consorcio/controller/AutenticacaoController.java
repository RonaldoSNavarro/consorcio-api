package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.DadosAutenticacao;
import br.com.estudo.consorcio.domain.dto.DadosTokenJWT;
import br.com.estudo.consorcio.domain.dto.DadosUsuarioLogado;
import br.com.estudo.consorcio.domain.dto.MfaVerifyRequest;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.service.MfaService;
import br.com.estudo.consorcio.service.TokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import org.springframework.security.core.Authentication;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;

@RestController
@RequestMapping("/api/login")
@Tag(name = "Autenticação", description = "Endpoint para login e geração de Token JWT")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final ExecutorService platformExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setName("bcrypt-auth-thread");
                t.setDaemon(true);
                return t;
            }
    );

    @org.springframework.beans.factory.annotation.Value("${api.security.cookie.secure:false}")
    private boolean secureCookie;

    private final MfaService mfaService;
    private final UsuarioRepository usuarioRepository;

    public AutenticacaoController(AuthenticationManager manager, TokenService tokenService, MfaService mfaService, UsuarioRepository usuarioRepository) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.mfaService = mfaService;
        this.usuarioRepository = usuarioRepository;
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        platformExecutor.shutdown();
    }

    @PostMapping
    public ResponseEntity<?> efetuarLogin(@Valid @RequestBody DadosAutenticacao dados) {
        System.out.println("[DEBUG-AUTH] Tentativa de login: usuário='" + dados.login() + "', senha='" + dados.senha() + "'");
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        
        // Delegação para Platform Threads (BCrypt) para evitar Starvation no Loom
        Authentication authentication;
        try {
            authentication = platformExecutor.submit(() -> manager.authenticate(authenticationToken)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegraDeNegocioException("Processamento de login interrompido");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.springframework.security.core.AuthenticationException) {
                throw (org.springframework.security.core.AuthenticationException) e.getCause();
            }
            throw new RegraDeNegocioException("Erro na autenticação: " + e.getCause().getMessage());
        }

        Usuario usuario = (Usuario) authentication.getPrincipal();

        if (usuario.isMfaEnabled()) {
            mfaService.enviarCodigoMfa(usuario);
            String tempToken = tokenService.gerarTokenMfaTemporario(usuario);
            return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                    .body(java.util.Map.of("mfaRequired", true, "tempToken", tempToken));
        }

        var tokenJWT = tokenService.gerarToken(usuario);

        ResponseCookie cookie = ResponseCookie.from("token", tokenJWT)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(7200) // 2 horas
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/mfa-verify")
    public ResponseEntity<Void> mfaVerify(@Valid @RequestBody MfaVerifyRequest request) {
        var jwt = tokenService.verificarTokenMfaTemporario(request.tempToken());
        String username = jwt.getSubject();
        
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(username);
        if (usuario == null || !usuario.isMfaEnabled()) {
            throw new RegraDeNegocioException("MFA não configurado ou usuário não encontrado.");
        }

        boolean isValid = mfaService.verifyCode(usuario, request.code());
        if (!isValid) {
            throw new RegraDeNegocioException("Código MFA inválido");
        }

        var tokenJWT = tokenService.gerarToken(usuario);

        ResponseCookie cookie = ResponseCookie.from("token", tokenJWT)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(7200) // 2 horas
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> efetuarLogout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<DadosUsuarioLogado> obterUsuarioLogado() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        var usuario = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(new DadosUsuarioLogado(usuario.getUsername(), usuario.getRole(), usuario.getNome(), usuario.getEmail(), usuario.isMfaEnabled()));
    }
}