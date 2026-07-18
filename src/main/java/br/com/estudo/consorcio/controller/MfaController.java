package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.MfaConfirmRequest;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.service.MfaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mfa")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "MFA", description = "Endpoint para configuração de Autenticação em Duas Etapas por E-mail")
public class MfaController {

    private final MfaService mfaService;
    private final UsuarioRepository usuarioRepository;

    public MfaController(MfaService mfaService, UsuarioRepository usuarioRepository) {
        this.mfaService = mfaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<Void> setupMfa(@AuthenticationPrincipal Usuario principal) {
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(principal.getUsername());
        if (usuario == null) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("Usuário não encontrado");
        }

        if (usuario.isMfaEnabled()) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("MFA já está habilitado para este usuário.");
        }

        // Envia o código MFA de pareamento para o e-mail do usuário
        mfaService.enviarCodigoMfa(usuario);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<Void> confirmMfa(@AuthenticationPrincipal Usuario principal, @RequestBody @Valid MfaConfirmRequest request) {
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(principal.getUsername());
        if (usuario == null) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("Usuário não encontrado");
        }

        if (usuario.isMfaEnabled()) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("MFA já está habilitado para este usuário.");
        }

        boolean isValid = mfaService.verifyCode(usuario, request.code());

        if (isValid) {
            usuario.setMfaEnabled(true);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok().build();
        }

        throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("Código MFA inválido. Tente novamente.");
    }

    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<Void> resetMfa(@AuthenticationPrincipal Usuario principal) {
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(principal.getUsername());
        if (usuario == null) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("Usuário não encontrado");
        }

        usuario.setMfaEnabled(false);
        usuario.setMfaCode(null);
        usuario.setMfaCodeExpiresAt(null);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok().build();
    }
}
