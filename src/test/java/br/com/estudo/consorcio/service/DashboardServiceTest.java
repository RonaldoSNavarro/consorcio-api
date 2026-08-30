package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.EvolucaoMensalDTO;
import br.com.estudo.consorcio.domain.dto.GrupoHeatmapDTO;
import br.com.estudo.consorcio.domain.dto.KpiFinanceiroDTO;
import br.com.estudo.consorcio.domain.dto.ProjecaoReceitaDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private MovimentoFinanceiroService movimentoFinanceiroService;

    @InjectMocks
    private DashboardService service;

    @Test
    @DisplayName("Deve calcular KPIs consolidados com sucesso")
    void deveCalcularKpis() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setValorCredito(new BigDecimal("100000.00"));

        Cota cota = new Cota();
        cota.setId(10L);
        cota.setStatus(StatusCota.ATIVA);
        cota.setGrupo(grupo);

        Parcela parcela = new Parcela();
        parcela.setId(100L);
        parcela.setStatus(StatusParcela.PAGA);
        parcela.setValorParcela(new BigDecimal("1200.00"));
        parcela.setValorTaxaAdministracao(new BigDecimal("180.00"));
        parcela.setValorFundoComum(new BigDecimal("950.00"));
        parcela.setValorFundoReserva(new BigDecimal("70.00"));

        when(grupoRepository.findAll()).thenReturn(List.of(grupo));
        when(cotaRepository.findAll()).thenReturn(List.of(cota));
        when(parcelaRepository.findAll()).thenReturn(List.of(parcela));

        KpiFinanceiroDTO kpis = service.obterKpis();

        assertNotNull(kpis);
        assertEquals(new BigDecimal("1200.00"), kpis.arrecadacaoTotal());
        assertEquals(new BigDecimal("180.00"), kpis.receitaTaxaAdminAcumulada());
    }

    @Test
    @DisplayName("Deve obter evolução de tendências dos últimos 6 meses")
    void deveObterTendencias() {
        List<EvolucaoMensalDTO> evolucao = service.obterTendencias();

        assertNotNull(evolucao);
        assertEquals(6, evolucao.size());
    }

    @Test
    @DisplayName("Deve gerar heatmap dos grupos")
    void deveGerarHeatmap() {
        Grupo g = new Grupo();
        g.setId(1L);
        g.setCodigoGrupo("GRP-01");
        g.setStatus(StatusGrupo.EM_ANDAMENTO);

        when(grupoRepository.findAll()).thenReturn(List.of(g));
        when(movimentoFinanceiroService.obterSaldoGrupo(1L)).thenReturn(new BigDecimal("250000.00"));

        List<GrupoHeatmapDTO> heatmap = service.obterHeatmapGrupos();

        assertNotNull(heatmap);
        assertEquals(1, heatmap.size());
        assertEquals("GRP-01", heatmap.get(0).codigoGrupo());
    }

    @Test
    @DisplayName("Deve gerar projeções de receita de TA")
    void deveGerarProjecoes() {
        List<ProjecaoReceitaDTO> projecoes = service.obterProjecoes();

        assertNotNull(projecoes);
        assertEquals(6, projecoes.size());
    }
}