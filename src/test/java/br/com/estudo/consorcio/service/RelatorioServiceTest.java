package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AlertaPldFtResponseDTO;
import br.com.estudo.consorcio.domain.dto.BalanceteResponseDTO;
import br.com.estudo.consorcio.domain.dto.EstatisticasGrupoResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private LanceRepository lanceRepository;

    @Mock
    private ContemplacaoRepository contemplacaoRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    @DisplayName("Deve gerar o balancete contábil (Doc 4110) com saldos corretos")
    void deveGerarBalanceteContabil() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setCodigo("GRP-4110");

        ContaContabil contaFC = new ContaContabil();
        contaFC.setCodigoCosif("2.1.2.10.10-6");
        contaFC.setNome("Fundo Comum");
        contaFC.setNatureza(NaturezaContabil.CREDORA);

        ContaContabil contaFR = new ContaContabil();
        contaFR.setCodigoCosif("2.1.2.10.20-9");
        contaFR.setNome("Fundo Reserva");
        contaFR.setNatureza(NaturezaContabil.CREDORA);

        when(grupoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(contabilidadeService.listarTodasContas()).thenReturn(List.of(contaFC, contaFR));
        when(contabilidadeService.calcularSaldoConta(grupo, "2.1.2.10.10-6")).thenReturn(new BigDecimal("150000.00"));
        when(contabilidadeService.calcularSaldoConta(grupo, "2.1.2.10.20-9")).thenReturn(new BigDecimal("15000.00"));

        LocalDate dataRef = LocalDate.now();

        // --- ACT ---
        BalanceteResponseDTO response = relatorioService.gerarBalancete(grupoId, dataRef);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(grupoId, response.grupoId());
        assertEquals("GRP-4110", response.codigoGrupo());
        assertEquals(2, response.contas().size());
        assertEquals(new BigDecimal("150000.00"), response.contas().get(0).saldo());
        assertEquals("Fundo Comum", response.contas().get(0).nome());
        assertEquals(new BigDecimal("15000.00"), response.contas().get(1).saldo());
    }

    @Test
    @DisplayName("Deve gerar estatísticas do grupo (Doc 2080)")
    void deveGerarEstatisticas() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setCodigo("GRP-2080");

        when(grupoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(cotaRepository.countByGrupoId(grupoId)).thenReturn(100L);
        when(cotaRepository.countByGrupoIdAndStatus(grupoId, StatusCota.CANCELADA)).thenReturn(5L);

        when(lanceRepository.countByGrupoIdAndPeriodo(eq(grupoId), any(), any())).thenReturn(50L);
        when(lanceRepository.countByGrupoIdAndStatusAndPeriodo(eq(grupoId), eq(StatusApuracaoLance.VENCEDOR), any(), any())).thenReturn(10L);

        when(contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(eq(grupoId), eq(TipoContemplacao.SORTEIO), any(), any())).thenReturn(2L);
        when(contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(eq(grupoId), eq(TipoContemplacao.LANCE_LIVRE), any(), any())).thenReturn(1L);
        when(contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(eq(grupoId), eq(TipoContemplacao.LANCE_FIXO), any(), any())).thenReturn(1L);
        when(contemplacaoRepository.somarCreditosLiberadosPorGrupoEPeriodo(eq(grupoId), any(), any())).thenReturn(new BigDecimal("200000.00"));

        LocalDate dataIni = LocalDate.of(2026, 1, 1);
        LocalDate dataFim = LocalDate.of(2026, 1, 31);

        // --- ACT ---
        EstatisticasGrupoResponseDTO response = relatorioService.gerarEstatisticas(grupoId, dataIni, dataFim);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(100L, response.totalAdesoes());
        assertEquals(5L, response.totalExclusoes());
        assertEquals(50L, response.totalLancesOfertados());
        assertEquals(10L, response.totalLancesVencedores());
        assertEquals(2L, response.totalContemplacoesSorteio());
        assertEquals(2L, response.totalContemplacoesLance()); // Livre + Fixo
        assertEquals(new BigDecimal("200000.00"), response.valorTotalCreditosLiberados());
    }

    @Test
    @DisplayName("Deve retornar alerta PLD/FT para lances iguais ou superiores a R$ 50.000")
    void deveGerarAlertaPldFt() {
        // --- ARRANGE ---
        Cliente cliente = new Cliente();
        cliente.setNome("John Doe");
        cliente.setCpfCnpj("111.222.333-44");

        Cota cota = new Cota();
        cota.setId(10L);
        cota.setCliente(cliente);

        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setCodigo("GRP-PLD");

        Assembleia assembleia = new Assembleia();
        assembleia.setGrupo(grupo);

        Lance lance = new Lance();
        lance.setId(100L);
        lance.setCota(cota);
        lance.setAssembleia(assembleia);
        lance.setTipo(TipoLance.FIRME);
        lance.setValorOferta(new BigDecimal("55000.00"));
        lance.setDataOferta(LocalDateTime.now());

        when(lanceRepository.findByValorOfertaGreaterThanEqualAndDataOfertaBetween(any(), any(), any()))
                .thenReturn(List.of(lance));

        LocalDateTime ini = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();

        // --- ACT ---
        List<AlertaPldFtResponseDTO> response = relatorioService.gerarAlertaPldFt(ini, fim);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(100L, response.get(0).lanceId());
        assertEquals("John Doe", response.get(0).nomeConsorciado());
        assertEquals(new BigDecimal("55000.00"), response.get(0).valorOferta());
    }

    @Test
    @DisplayName("Deve lançar exceção se tentar gerar balancete de grupo inexistente")
    void deveLancarExcecaoGerarBalanceteInexistente() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> relatorioService.gerarBalancete(99L, LocalDate.now()));
    }
}
