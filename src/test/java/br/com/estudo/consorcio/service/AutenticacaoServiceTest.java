package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private AutenticacaoService service;

    @Test
    @DisplayName("Deve retornar o usuário (UserDetails) quando o login for encontrado no banco")
    void deveRetornarUsuarioQuandoEncontrado() {
        // --- ARRANGE ---
        String usernameProcurado = "admin@consorcio.com";

        // Em vez de instanciar a sua entidade Usuario real (que pode ter muitos campos obrigatórios),
        // nós criamos um "dublê" rápido da interface UserDetails usando o próprio Mockito.
        UserDetails mockUsuario = mock(UserDetails.class);

        // Ensinamos o repositório a devolver o nosso dublê quando procurarem pelo login
        when(repository.findByLogin(usernameProcurado)).thenReturn(mockUsuario);

        // --- ACT ---
        UserDetails resultado = service.loadUserByUsername(usernameProcurado);

        // --- ASSERT ---
        assertNotNull(resultado, "O UserDetails retornado não pode ser nulo");
        assertEquals(mockUsuario, resultado, "O usuário retornado deve ser o mesmo encontrado no banco");

        // Verifica se o repositório foi chamado exatamente uma vez com o username correto
        verify(repository, times(1)).findByLogin(usernameProcurado);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o login não existir no banco")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        // --- ARRANGE ---
        String usernameFantasma = "hacker_invisivel";

        // Simulamos o comportamento do Spring Data JPA quando não encontra um registro (retorna null)
        when(repository.findByLogin(usernameFantasma)).thenReturn(null);

        // --- ACT & ASSERT ---
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername(usernameFantasma);
        });

        // Verificamos se a mensagem da exceção é exatamente a que o Spring Security espera para logar a falha
        assertEquals("Usuário não encontrado: " + usernameFantasma, exception.getMessage());

        verify(repository, times(1)).findByLogin(usernameFantasma);
    }
}