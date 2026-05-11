package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.DadosAutenticacao;
import br.com.estudo.consorcio.domain.dto.DadosTokenJWT;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.service.TokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<DadosTokenJWT> efetuarLogin(@RequestBody DadosAutenticacao dados) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());

        // O Spring Security vai pegar esse token, ir no AutenticacaoService, buscar no banco e comparar as senhas (Hashes)
        var authentication = manager.authenticate(authenticationToken);

        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }
}