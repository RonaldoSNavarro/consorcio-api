package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ContratoResponseDTO;
import br.com.estudo.consorcio.domain.dto.TipoVendaRequestDTO;
import br.com.estudo.consorcio.domain.enums.TipoVendaEnum;
import br.com.estudo.consorcio.domain.mapper.PropostaAdesaoMapper;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.domain.repository.ProdutoConsorcioRepository;
import br.com.estudo.consorcio.domain.repository.TipoVendaRepository;
import br.com.estudo.consorcio.domain.service.PropostaAdesaoService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_VENDAS", "MANAGE_GRUPOS", "VIEW_GRUPOS", "MANAGE_COMPLIANCE"})
class VendasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private PropostaAdesaoService propostaService;

    @MockitoBean
    private PropostaAdesaoMapper mapper;

    @MockitoBean
    private TipoVendaRepository tipoVendaRepository;

    @MockitoBean
    private ProdutoConsorcioRepository produtoRepository;

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
    @DisplayName("Deve listar tipos de venda com sucesso")
    void deveListarTiposVenda() throws Exception {
        TipoVenda tv = new TipoVenda();
        tv.setId(1L);
        tv.setNome("Venda Balcão");
        when(tipoVendaRepository.findAll()).thenReturn(List.of(tv));

        mockMvc.perform(get("/api/vendas/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Venda Balcão"));
    }

    @Test
    @DisplayName("Deve criar tipo de venda com sucesso")
    void deveCriarTipoVenda() throws Exception {
        TipoVendaRequestDTO dto = new TipoVendaRequestDTO("Digital", "Canal App", TipoVendaEnum.DIGITAL_SELF_SERVICE, new BigDecimal("0.05"), false, true, true);
        TipoVenda salvo = new TipoVenda();
        salvo.setId(2L);
        salvo.setNome("Digital");

        when(tipoVendaRepository.save(any(TipoVenda.class))).thenReturn(salvo);

        mockMvc.perform(post("/api/vendas/tipos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @DisplayName("Deve aprovar proposta com sucesso")
    void deveAprovarProposta() throws Exception {
        ContratoAdesao contrato = new ContratoAdesao();
        contrato.setId(5L);
        contrato.setNumeroContrato("CTR-001");

        ContratoResponseDTO respDTO = ContratoResponseDTO.builder()
                .id(5L)
                .numeroContrato("CTR-001")
                .status("ASSINADO")
                .build();

        when(propostaService.aprovarProposta(10L)).thenReturn(contrato);
        when(mapper.toContratoResponse(contrato)).thenReturn(respDTO);

        mockMvc.perform(post("/api/vendas/propostas/10/aprovar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroContrato").value("CTR-001"));
    }
}