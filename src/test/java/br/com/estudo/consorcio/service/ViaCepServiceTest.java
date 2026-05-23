package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViaCepServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ViaCepService viaCepService;

    @BeforeEach
    void setUp() {
        viaCepService = new ViaCepService(restClient);
    }

    @Test
    @DisplayName("Deve buscar CEP com sucesso preenchendo todos os campos")
    void deveBuscarCepComSucesso() {
        // --- ARRANGE ---
        String cep = "01001000";
        ViaCepResponseDTO mockResponse = new ViaCepResponseDTO(
                "01001-000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP", false
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/{cep}/json/"), eq(cep))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ViaCepResponseDTO.class)).thenReturn(mockResponse);

        // --- ACT ---
        ViaCepResponseDTO response = viaCepService.buscarCep(cep);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals("01001-000", response.cep());
        assertEquals("Praça da Sé", response.logradouro());
        assertEquals("Sé", response.bairro());
        assertEquals("São Paulo", response.localidade());
        assertEquals("SP", response.uf());
        assertFalse(Boolean.TRUE.equals(response.erro()));
    }

    @Test
    @DisplayName("Deve lançar exceção se o formato do CEP for inválido")
    void deveLancarExcecaoCepInvalido() {
        // Formatos inválidos (letras, tamanho errado)
        assertThrows(RegraDeNegocioException.class, () -> viaCepService.buscarCep("123"));
        assertThrows(RegraDeNegocioException.class, () -> viaCepService.buscarCep("123456789"));
        assertThrows(RegraDeNegocioException.class, () -> viaCepService.buscarCep("abc12345"));
        assertThrows(RegraDeNegocioException.class, () -> viaCepService.buscarCep(null));
    }

    @Test
    @DisplayName("Deve lançar exceção se a API do ViaCEP retornar erro")
    void deveLancarExcecaoSeApiRetornarErro() {
        // --- ARRANGE ---
        String cep = "99999999";
        ViaCepResponseDTO mockResponse = new ViaCepResponseDTO(
                null, null, null, null, null, null, true
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/{cep}/json/"), eq(cep))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ViaCepResponseDTO.class)).thenReturn(mockResponse);

        // --- ACT & ASSERT ---
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class, 
                () -> viaCepService.buscarCep(cep)
        );

        assertEquals("CEP não encontrado: 99999999", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção se a API do ViaCEP estiver indisponível ou falhar")
    void deveLancarExcecaoSeApiFalhar() {
        // --- ARRANGE ---
        String cep = "01001000";

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/{cep}/json/"), eq(cep))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ViaCepResponseDTO.class)).thenThrow(new RuntimeException("Connection timeout"));

        // --- ACT & ASSERT ---
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class, 
                () -> viaCepService.buscarCep(cep)
        );

        assertTrue(exception.getMessage().contains("Falha ao integrar com a API ViaCEP: Connection timeout"));
    }
}
