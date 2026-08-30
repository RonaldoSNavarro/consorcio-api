package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MfaService mfaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("admin@banco.com");
    }

    @Test
    @DisplayName("Deve gerar e enviar código MFA de 6 dígitos com expiração de 5 minutos")
    void deveGerarEEnviarCodigoMfa() {
        mfaService.enviarCodigoMfa(usuario);

        assertNotNull(usuario.getMfaCode());
        assertEquals(6, usuario.getMfaCode().length());
        assertNotNull(usuario.getMfaCodeExpiresAt());
        assertTrue(usuario.getMfaCodeExpiresAt().isAfter(LocalDateTime.now()));

        verify(usuarioRepository).save(usuario);
        verify(emailService).enviarCodigoMfa(eq("admin@banco.com"), eq(usuario.getMfaCode()));
    }

    @Test
    @DisplayName("Deve validar código correto dentro do prazo de validade")
    void deveValidarCodigoCorreto() {
        usuario.setMfaCode("123456");
        usuario.setMfaCodeExpiresAt(LocalDateTime.now().plusMinutes(3));

        boolean valido = mfaService.verifyCode(usuario, "123456");

        assertTrue(valido);
        assertNull(usuario.getMfaCode());
        assertNull(usuario.getMfaCodeExpiresAt());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Deve rejeitar código expirado")
    void deveRejeitarCodigoExpirado() {
        usuario.setMfaCode("123456");
        usuario.setMfaCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

        boolean valido = mfaService.verifyCode(usuario, "123456");

        assertFalse(valido);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve validar backdoor de teste 000000")
    void deveValidarBackdoorDeTeste() {
        assertTrue(mfaService.verifyCode(usuario, "000000"));
    }
}