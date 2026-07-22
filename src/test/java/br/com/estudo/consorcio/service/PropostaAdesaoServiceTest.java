package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ContratoAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.PropostaAdesaoRepository;
import br.com.estudo.consorcio.domain.service.PropostaAdesaoService;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PropostaAdesaoServiceTest {

    @Mock
    private PropostaAdesaoRepository propostaRepository;

    @Mock
    private ContratoAdesaoRepository contratoRepository;

    @Mock
    private AlertaComplianceRepository alertaComplianceRepository;

    @Mock
    private br.com.estudo.consorcio.domain.repository.GrupoRepository grupoRepository;

    @Mock
    private br.com.estudo.consorcio.domain.repository.CotaRepository cotaRepository;

    @Mock
    private br.com.estudo.consorcio.domain.repository.AssembleiaRepository assembleiaRepository;

    @Mock
    private br.com.estudo.consorcio.domain.repository.ParcelaRepository parcelaRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private PropostaAdesaoService propostaService;

    private PropostaAdesao proposta;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNivelRisco(NivelRisco.MEDIO);

        proposta = new PropostaAdesao();
        proposta.setId(10L);
        proposta.setCliente(cliente);
        proposta.setStatus(StatusProposta.EM_ANALISE);

        when(clock.instant()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void aprovarProposta_ClienteAltoRisco_DeveLancarExcecaoEMudarStatus() {
        cliente.setNivelRisco(NivelRisco.ALTO);

        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta));
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(any(), any())).thenReturn(false);

        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> {
            propostaService.aprovarProposta(10L);
        });

        assertEquals("Proposta encaminhada para análise manual de risco (Compliance).", exception.getMessage());
        assertEquals(StatusProposta.PENDENTE_ANALISE_RISCO, proposta.getStatus());
        verify(propostaRepository).save(proposta);
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void analisarPropostaRisco_Aprovada_DeveGerarContrato() {
        proposta.setStatus(StatusProposta.PENDENTE_ANALISE_RISCO);

        br.com.estudo.consorcio.domain.model.CategoriaBem cat = new br.com.estudo.consorcio.domain.model.CategoriaBem();
        cat.setTipoBacen(br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen.BEM_IMOVEL);

        br.com.estudo.consorcio.domain.model.BemReferencia bem = new br.com.estudo.consorcio.domain.model.BemReferencia();
        bem.setCategoriaBem(cat);

        br.com.estudo.consorcio.domain.model.ProdutoConsorcio produto = new br.com.estudo.consorcio.domain.model.ProdutoConsorcio();
        produto.setPrazoMeses(36);
        produto.setBemReferencia(bem);
        produto.setTaxaAdministracaoPerc(new java.math.BigDecimal("10.00"));
        proposta.setProduto(produto);
        proposta.setValorCreditoSolicitado(new java.math.BigDecimal("100000.00"));

        br.com.estudo.consorcio.domain.model.Grupo grupoMock = new br.com.estudo.consorcio.domain.model.Grupo();
        grupoMock.setId(1L);
        grupoMock.setStatus(br.com.estudo.consorcio.domain.model.StatusGrupo.EM_ANDAMENTO);
        grupoMock.setTaxaAdministracao(new java.math.BigDecimal("10.00"));
        grupoMock.setDiasAntecedenciaVencimento(5);

        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta));
        when(grupoRepository.encontrarMelhorGrupoDisponivel(any())).thenReturn(Optional.of(grupoMock));
        when(contratoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ContratoAdesao contrato = propostaService.analisarPropostaRisco(10L, new br.com.estudo.consorcio.domain.dto.AnaliseRiscoRequestDTO(true, "Tudo ok"));

        assertNotNull(contrato);
        assertEquals(StatusProposta.APROVADA, proposta.getStatus());
        verify(propostaRepository).save(proposta);
        verify(contratoRepository, times(2)).save(any(ContratoAdesao.class));
    }

    @Test
    void analisarPropostaRisco_Reprovada_DeveMudarStatusERetornarNull() {
        PropostaAdesao proposta = new PropostaAdesao();
        proposta.setStatus(StatusProposta.PENDENTE_ANALISE_RISCO);
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta));

        ContratoAdesao contrato = propostaService.analisarPropostaRisco(10L, new br.com.estudo.consorcio.domain.dto.AnaliseRiscoRequestDTO(false, "Histórico suspeito"));

        assertNull(contrato);
        assertEquals(StatusProposta.REPROVADA_POR_RISCO, proposta.getStatus());
        verify(propostaRepository).save(proposta);
        verify(contratoRepository, never()).save(any());
    }
}
