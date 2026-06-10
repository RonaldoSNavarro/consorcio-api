package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.DadosAutenticacao;
import br.com.estudo.consorcio.domain.dto.DadosTokenJWT;
import br.com.estudo.consorcio.domain.dto.DadosUsuarioLogado;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.service.TokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
@Tag(name = "Autenticação", description = "Endpoint para login e geração de Token JWT")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;

    public AutenticacaoController(AuthenticationManager manager, TokenService tokenService) {
        this.manager = manager;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<Void> efetuarLogin(@Valid @RequestBody DadosAutenticacao dados) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = manager.authenticate(authenticationToken);
        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        ResponseCookie cookie = ResponseCookie.from("token", tokenJWT)
                .httpOnly(true)
                .secure(false) // Para produção usar true com HTTPS
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
                .secure(false)
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
        return ResponseEntity.ok(new DadosUsuarioLogado(usuario.getUsername(), usuario.getRole()));
    }
}