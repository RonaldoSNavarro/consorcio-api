package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.MfaConfirmRequest;
import br.com.estudo.consorcio.domain.dto.MfaSetupResponse;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.service.MfaService;
import dev.samstevens.totp.exceptions.QrGenerationException;
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
@Tag(name = "MFA", description = "Endpoint para configuração de Autenticação em Duas Etapas (TOTP)")
public class MfaController {

    private final MfaService mfaService;
    private final UsuarioRepository usuarioRepository;

    public MfaController(MfaService mfaService, UsuarioRepository usuarioRepository) {
        this.mfaService = mfaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/setup")
    @Transactional
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal Usuario usuario) throws QrGenerationException {
        // Se o usuário não tem um secret ainda, gera um
        if (usuario.getMfaSecret() == null || usuario.getMfaSecret().isBlank()) {
            usuario.setMfaSecret(mfaService.generateSecret());
            usuarioRepository.save(usuario);
        }

        String qrCode = mfaService.generateQrCodeImageUri(
                usuario.getMfaSecret(),
                usuario.getEmail() != null ? usuario.getEmail() : usuario.getUsername(),
                "Consórcio API"
        );

        return ResponseEntity.ok(new MfaSetupResponse(usuario.getMfaSecret(), qrCode));
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<Void> confirmMfa(@AuthenticationPrincipal Usuario usuario, @RequestBody @Valid MfaConfirmRequest request) {
        if (usuario.isMfaEnabled()) {
            throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("MFA já está habilitado para este usuário.");
        }

        boolean isValid = mfaService.verifyCode(usuario.getMfaSecret(), request.code());

        if (isValid) {
            usuario.setMfaEnabled(true);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok().build();
        }

        throw new br.com.estudo.consorcio.exception.RegraDeNegocioException("Código MFA inválido. Tente novamente.");
    }
}
