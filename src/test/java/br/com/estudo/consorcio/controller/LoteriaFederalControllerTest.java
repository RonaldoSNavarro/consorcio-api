package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.LoteriaFederalDTO;
import br.com.estudo.consorcio.domain.model.LoteriaFederal;
import br.com.estudo.consorcio.domain.repository.LoteriaFederalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoteriaFederalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_GRUPOS", "MANAGE_GRUPOS"})
class LoteriaFederalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private LoteriaFederalRepository repository;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionService intrusionDetectionService;

    @MockitoBean
    private br.com.estudo.consorcio.config.SecurityFilter securityFilter;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private br.com.estudo.consorcio.service.TokenService tokenService;

    @MockitoBean
    private br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    @Test
    @DisplayName("Deve registrar extrato da loteria federal com sucesso")
    void deveRegistrarExtrato() throws Exception {
        LoteriaFederalDTO dto = new LoteriaFederalDTO(
                null, "5900", LocalDate.now(), "12345", "23456", "34567", "45678", "56789"
        );
        LoteriaFederal salvo = new LoteriaFederal();
        salvo.setId(1L);
        salvo.setConcurso("5900");
        salvo.setDataSorteio(LocalDate.now());
        salvo.setPremio1("12345");

        when(repository.save(any(LoteriaFederal.class))).thenReturn(salvo);

        mockMvc.perform(post("/api/loteria-federal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.concurso").value("5900"));
    }

    @Test
    @DisplayName("Deve listar todos os sorteios da loteria federal")
    void deveListarSorteios() throws Exception {
        LoteriaFederal lf = new LoteriaFederal();
        lf.setId(1L);
        lf.setConcurso("5900");
        lf.setDataSorteio(LocalDate.now());

        when(repository.findAll()).thenReturn(List.of(lf));

        mockMvc.perform(get("/api/loteria-federal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].concurso").value("5900"));
    }
}