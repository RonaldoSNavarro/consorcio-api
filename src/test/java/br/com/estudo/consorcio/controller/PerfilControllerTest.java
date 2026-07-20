package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.PerfilRequestDTO;
import br.com.estudo.consorcio.domain.dto.PerfilResponseDTO;
import br.com.estudo.consorcio.domain.model.Permissao;
import br.com.estudo.consorcio.service.PerfilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerfilController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
public class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilService perfilService;

    @MockitoBean
    private br.com.estudo.consorcio.config.SecurityFilter securityFilter;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private br.com.estudo.consorcio.service.TokenService tokenService;

    @MockitoBean
    private br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    private PerfilResponseDTO perfilResponseDTO;

    @BeforeEach
    public void setup() {
        perfilResponseDTO = new PerfilResponseDTO(1L, "ADMIN", Set.of(Permissao.MANAGE_USERS));
    }

    @Test
    @WithMockUser(authorities = {"VIEW_DASHBOARD"})
    public void deveListarPerfisComPermissao() throws Exception {
        Mockito.when(perfilService.listarTodos()).thenReturn(List.of(perfilResponseDTO));

        mockMvc.perform(get("/api/perfis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_USERS"})
    public void deveSalvarPerfilComPermissao() throws Exception {
        Mockito.when(perfilService.salvar(any())).thenReturn(perfilResponseDTO);

        mockMvc.perform(post("/api/perfis")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"ADMIN\",\"permissoes\":[\"MANAGE_USERS\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = {"VIEW_DASHBOARD"})
    public void naoDeveSalvarPerfilSemPermissao() throws Exception {
        mockMvc.perform(post("/api/perfis")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"ADMIN\",\"permissoes\":[\"MANAGE_USERS\"]}"))
                .andExpect(status().isForbidden());
    }
}
