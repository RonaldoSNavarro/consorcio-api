package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // Padrão correto para Spring Boot 3.4+ / 4.x
    private ClienteService clienteService;

    // ========================================================================
    // TESTES DE CADASTRO (POST)
    // ========================================================================

    @Test
    @DisplayName("Deve devolver 201 Created e o JSON do cliente ao salvar com sucesso")
    @WithMockUser
    void deveRetornar201AoSalvarCliente() throws Exception {
        // Arrange
        ClienteRequestDTO request = new ClienteRequestDTO("João", "11122233344", "joao@email.com", "1199999999");
        ClienteResponseDTO response = new ClienteResponseDTO(1L, "João", "joao@email.com", LocalDateTime.now());

        when(clienteService.salvar(any(ClienteRequestDTO.class))).thenReturn(response);
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("João"))
                .andExpect(jsonPath("$.cpfCnpj").doesNotExist());
    }

    @Test
    @DisplayName("Deve devolver 400 Bad Request se o DTO enviado for inválido (Validação do Bean)")
    @WithMockUser
    void deveRetornar400ParaDadosInvalidos() throws Exception {
        // Arrange
        // Enviando um nome em branco e um CPF vazio (Isto deve acionar o @NotBlank e @Pattern do seu DTO)
        ClienteRequestDTO requestInvalido = new ClienteRequestDTO("", "", "email-errado", null);
        String jsonRequest = objectMapper.writeValueAsString(requestInvalido);

        // Act & Assert
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest()); // O Spring Boot intercepta e lança 400 antes mesmo de chegar no Service
    }

    // ========================================================================
    // TESTES DE LISTAGEM (GET)
    // ========================================================================

    @Test
    @DisplayName("Deve devolver 200 OK e a lista de clientes")
    @WithMockUser
    void deveRetornar200AoListarClientes() throws Exception {
        // Arrange
        ClienteResponseDTO cliente1 = new ClienteResponseDTO(1L, "Ronaldo", "ronaldo@email.com", LocalDateTime.now());
        ClienteResponseDTO cliente2 = new ClienteResponseDTO(2L, "Maria", "maria@email.com", LocalDateTime.now());

        when(clienteService.listarTodos()).thenReturn(List.of(cliente1, cliente2));

        // Act & Assert
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()) // Verifica se a resposta é um Array JSON []
                .andExpect(jsonPath("$.length()").value(2)) // Verifica se tem 2 itens
                .andExpect(jsonPath("$[0].nome").value("Ronaldo"))
                .andExpect(jsonPath("$[1].nome").value("Maria"));
    }

    // ========================================================================
    // TESTES DE SEGURANÇA (SECURITY)
    // ========================================================================

    @Test
    @DisplayName("Deve devolver 403 Forbidden se tentar acessar qualquer endpoint sem estar autenticado")
    void deveRetornar403SemAutenticacao() throws Exception {
        // Testando POST sem @WithMockUser
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Testando GET sem @WithMockUser
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }
}