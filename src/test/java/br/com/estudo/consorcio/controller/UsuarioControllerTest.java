package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.PerfilResponseDTO;
import br.com.estudo.consorcio.domain.dto.UsuarioResponseDTO;
import br.com.estudo.consorcio.domain.model.Permissao;
import br.com.estudo.consorcio.service.UsuarioService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private br.com.estudo.consorcio.config.SecurityFilter securityFilter;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private br.com.estudo.consorcio.service.TokenService tokenService;

    @MockitoBean
    private br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    private UsuarioResponseDTO usuarioResponseDTO;

    @BeforeEach
    public void setup() {
        PerfilResponseDTO perfil = new PerfilResponseDTO(1L, "ADMIN", Set.of(Permissao.MANAGE_USERS));
        usuarioResponseDTO = new UsuarioResponseDTO(1L, "admin", "Admin", "admin@teste.com", perfil);
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_USERS"})
    public void deveListarUsuariosComPermissao() throws Exception {
        Mockito.when(usuarioService.listarTodos()).thenReturn(List.of(usuarioResponseDTO));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    @WithMockUser(authorities = {"MANAGE_USERS"})
    public void deveSalvarUsuarioComPermissao() throws Exception {
        Mockito.when(usuarioService.salvar(any())).thenReturn(usuarioResponseDTO);

        mockMvc.perform(post("/api/usuarios")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"senha\":\"123\",\"perfilId\":1,\"nome\":\"Admin\",\"email\":\"admin@teste.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @WithMockUser(authorities = {"VIEW_DASHBOARD"})
    public void naoDeveListarUsuariosSemPermissao() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }
}
