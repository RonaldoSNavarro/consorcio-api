package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        // Injetamos uma chave secreta falsa para o teste rodar
        ReflectionTestUtils.setField(tokenService, "secret", "minhaChaveSecretaDeTeste123");
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido contendo 3 partes (Header, Payload, Signature)")
    void deveGerarTokenComSucesso() {
        // --- ARRANGE ---
        // Como Usuario implementa UserDetails, criamos um mock rápido para não precisarmos preencher todos os dados
        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioMock.getUsername()).thenReturn("admin@consorcio.com");

        // --- ACT ---
        String token = tokenService.gerarToken(usuarioMock);

        // --- ASSERT ---
        assertNotNull(token, "O token não pode ser nulo");
        assertFalse(token.isBlank(), "O token não pode estar vazio");

        // Um token JWT real é sempre dividido em 3 partes separadas por um ponto (.)
        assertEquals(3, token.split("\\.").length, "O token deve ter o formato padrão JWT");
    }

    @Test
    @DisplayName("Deve extrair corretamente o username (subject) de um token JWT válido")
    void deveRecuperarSubjectComSucesso() {
        // --- ARRANGE ---
        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioMock.getUsername()).thenReturn("admin@consorcio.com");

        // Usamos o próprio serviço para gerar um token real (Criptografia)
        String tokenValido = tokenService.gerarToken(usuarioMock);

        // --- ACT ---
        // Descriptografamos o token
        String subject = tokenService.getSubject(tokenValido);

        // --- ASSERT ---
        assertEquals("admin@consorcio.com", subject, "O subject extraído deve ser idêntico ao username criptografado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar decodificar um token falso ou alterado")
    void deveLancarExcecaoParaTokenInvalido() {
        // --- ARRANGE ---
        String tokenInvalido = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payloadFalso.assinaturaFalsa";

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tokenService.getSubject(tokenInvalido);
        });

        assertEquals("Token JWT inválido ou expirado!", exception.getMessage());
    }
}