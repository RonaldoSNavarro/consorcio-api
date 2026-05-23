package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaInadimplenciaResponseDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelaServiceTest {

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private MovimentoFinanceiroService movimentoService;

    @Mock
    private HistoricoConsorciadoService historicoService;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.ParcelaMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.ParcelaMapper.class);

    @InjectMocks
    private ParcelaService service;

    @Test
    @DisplayName("Deve calcular multa e juros corretamente quando o pagamento for em atraso")
    void deveCalcularMultaEJurosQuandoPagamentoEmAtraso() {
        // --- ARRANGE (Organização do cenário) ---
        Long idParcela = 1L;
        Long idCota = 10L;

        Cota cota = new Cota();
        cota.setId(idCota);

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cota.setCliente(cliente);

        Grupo grupo = new Grupo();
        grupo.setId(20L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        cota.setGrupo(grupo);

        // Cenário: Vencimento há 10 dias atrás para forçar o cálculo de atraso
        LocalDate vencimento = LocalDate.now().minusDays(10);
        LocalDate dataPagamento = LocalDate.now();

        Parcela parcela = new Parcela();
        parcela.setId(idParcela);
        parcela.setCota(cota);
        parcela.setValorFundoComum(new BigDecimal("1000.00"));
        parcela.setValorTaxaAdministracao(new BigDecimal("150.00"));
        parcela.setValorFundoReserva(new BigDecimal("50.00"));
        parcela.setDataVencimento(vencimento);
        parcela.setStatus(StatusParcela.PENDENTE);

        // Método que soma FC + Taxa + Reserva (Total: 1200.00)
        parcela.calcularValorTotal();

        // Configuração dos comportamentos simulados (Mocks)
        when(parcelaRepository.findById(idParcela)).thenReturn(Optional.of(parcela));
        when(parcelaRepository.save(any(Parcela.class))).thenAnswer(i -> i.getArguments()[0]);

        // --- ACT (Execução da ação) ---
        ParcelaResponseDTO resultado = service.pagar(idParcela, dataPagamento);

        // --- ASSERT (Verificação dos resultados) ---
        assertNotNull(resultado);
        assertEquals(idCota, resultado.cotaId());
        assertEquals(StatusParcela.PAGA, resultado.status());

        // Verificação da Matemática Financeira:
        // Multa: 2% de 1200.00 = 24.00
        assertEquals(new BigDecimal("24.00"), resultado.valorMulta(), "Multa deve ser de 2%");

        // Juros: 1% ao mês (0.01 / 30 dias * 10 dias * 1200.00) = 4.00
        assertEquals(new BigDecimal("4.00"), resultado.valorJuros(), "Juros devem ser pro-rata die");

        // Total: 1200.00 + 24.00 + 4.00 = 1228.00
        assertEquals(new BigDecimal("1228.00"), resultado.valorPago(), "Valor total pago está incorreto");

        // Garante que o banco de dados foi atualizado
        verify(parcelaRepository, times(1)).save(any(Parcela.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar pagar uma parcela que já consta como PAGA")
    void deveLancarExcecaoQuandoParcelaJaEstiverPaga() {
        // --- ARRANGE ---
        Long idParcela = 1L;
        Parcela parcelaPaga = new Parcela();
        parcelaPaga.setStatus(StatusParcela.PAGA);

        when(parcelaRepository.findById(idParcela)).thenReturn(Optional.of(parcelaPaga));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.pagar(idParcela, LocalDate.now());
        });

        assertEquals("Esta parcela já consta como paga.", exception.getMessage());

        // Verifica que o save NUNCA foi chamado, protegendo a integridade dos dados
        verify(parcelaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve amortizar por diluição e aplicar a regra do centavo perdido na última parcela")
    void deveAmortizarPorDiluicaoComRegraDoCentavoPerdido() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        // Lance de R$ 1.000,00 para dividir em 3 parcelas
        BigDecimal lance = new BigDecimal("1000.00");

        // R$ 1000 / 3 = 333.3333... O sistema deve arredondar para 333.33 (para as duas primeiras)
        // A última parcela deve receber o "troco" para fechar a conta exata: 333.34

        Parcela p1 = new Parcela(); p1.setNumeroParcela(1); p1.setValorFundoComum(new BigDecimal("1000.00"));
        Parcela p2 = new Parcela(); p2.setNumeroParcela(2); p2.setValorFundoComum(new BigDecimal("1000.00"));
        Parcela p3 = new Parcela(); p3.setNumeroParcela(3); p3.setValorFundoComum(new BigDecimal("1000.00"));

        List<Parcela> parcelasPendentes = List.of(p1, p2, p3);

        when(parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaAsc(cotaId, StatusParcela.PENDENTE))
                .thenReturn(parcelasPendentes);

        // --- ACT ---
        service.amortizarPorDiluicao(cotaId, lance);

        // --- ASSERT ---
        // P1 Fundo Comum Novo = 1000.00 - 333.33 = 666.67
        assertEquals(new BigDecimal("666.67"), p1.getValorFundoComum());

        // P2 Fundo Comum Novo = 1000.00 - 333.33 = 666.67
        assertEquals(new BigDecimal("666.67"), p2.getValorFundoComum());

        // P3 (Última) Fundo Comum Novo = 1000.00 - 333.34 = 666.66
        assertEquals(new BigDecimal("666.66"), p3.getValorFundoComum(), "A última parcela deve absorver o centavo perdido");

        // Verifica se o repositório foi chamado para salvar a lista inteira de uma vez
        verify(parcelaRepository, times(1)).saveAll(parcelasPendentes);
    }

    @Test
    @DisplayName("Deve amortizar reduzindo o prazo (quitando de trás para frente) e usar o troco na parcela seguinte")
    void deveAmortizarPorReducaoDePrazo() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        // Lance de R$ 1.500,00
        BigDecimal lance = new BigDecimal("1500.00");

        // Simulamos 3 parcelas de R$ 1200,00 totais (sendo 1000 de Fundo Comum)
        // Como o lance é de 1500, ele deve:
        // 1. Quitar a P60 inteira (sobram 300 do lance)
        // 2. Abater os 300 restantes do Fundo Comum da P59
        // 3. Não fazer nada com a P58

        Parcela p60 = new Parcela(); p60.setNumeroParcela(60); p60.setValorParcela(new BigDecimal("1200.00")); p60.setValorFundoComum(new BigDecimal("1000.00")); p60.setStatus(StatusParcela.PENDENTE);
        Parcela p59 = new Parcela(); p59.setNumeroParcela(59); p59.setValorParcela(new BigDecimal("1200.00")); p59.setValorFundoComum(new BigDecimal("1000.00")); p59.setStatus(StatusParcela.PENDENTE);
        Parcela p58 = new Parcela(); p58.setNumeroParcela(58); p58.setValorParcela(new BigDecimal("1200.00")); p58.setValorFundoComum(new BigDecimal("1000.00")); p58.setStatus(StatusParcela.PENDENTE);

        // O repositório traz as parcelas de trás para frente (DESC)
        List<Parcela> parcelasDeTrasParaFrente = List.of(p60, p59, p58);

        when(parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaDesc(cotaId, StatusParcela.PENDENTE))
                .thenReturn(parcelasDeTrasParaFrente);

        // --- ACT ---
        service.amortizarPorReducaoDePrazo(cotaId, lance);

        // --- ASSERT ---
        // A Parcela 60 deve estar PAGA
        assertEquals(StatusParcela.PAGA, p60.getStatus(), "A última parcela deve ser totalmente quitada");
        assertEquals(new BigDecimal("1200.00"), p60.getValorPago());

        // A Parcela 59 deve ter seu Fundo Comum reduzido em R$ 300,00 (1000 - 300 = 700)
        assertEquals(StatusParcela.PENDENTE, p59.getStatus(), "A penúltima parcela continua pendente");
        assertEquals(new BigDecimal("700.00"), p59.getValorFundoComum(), "O troco do lance deve ser abatido do Fundo Comum");

        // A Parcela 58 deve continuar intacta com seus 1000 de Fundo Comum originais
        assertEquals(new BigDecimal("1000.00"), p58.getValorFundoComum());

        verify(parcelaRepository, times(1)).saveAll(parcelasDeTrasParaFrente);
    }

    @Test
    @DisplayName("Deve retornar cota adimplente quando não houver parcelas em atraso")
    void deveRetornarCotaAdimplenteQuandoNaoHouverParcelasEmAtraso() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setNumeroCota(100);

        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PENDENTE);
        p1.setDataVencimento(LocalDate.now().plusDays(5)); // Vence no futuro, portanto adimplente

        Parcela p2 = new Parcela();
        p2.setStatus(StatusParcela.PAGA);
        p2.setDataVencimento(LocalDate.now().minusDays(5)); // Paga, portanto adimplente

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));
        when(parcelaRepository.findByCotaId(cotaId)).thenReturn(List.of(p1, p2));

        // --- ACT ---
        CotaInadimplenciaResponseDTO response = service.obterInadimplenciaCota(cotaId);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(cotaId, response.cotaId());
        assertEquals(100, response.numeroCota());
        assertFalse(response.possuiInadimplencia());
        assertEquals(0, response.quantidadeParcelasAtrasadas());
        assertEquals(BigDecimal.ZERO, response.multaAcumulada());
        assertEquals(BigDecimal.ZERO, response.jurosAcumulados());
        assertEquals(BigDecimal.ZERO, response.saldoDevedorTotal());
    }

    @Test
    @DisplayName("Deve retornar cota inadimplente e calcular juros e multa pro-rata die corretamente")
    void deveRetornarCotaInadimplenteECalcularValores() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setNumeroCota(100);

        // Parcela atrasada há 10 dias
        // Valor total original: FC 1000 + TaxaAdmin 150 + FundoReserva 50 = 1200.00
        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PENDENTE);
        p1.setValorFundoComum(new BigDecimal("1000.00"));
        p1.setValorTaxaAdministracao(new BigDecimal("150.00"));
        p1.setValorFundoReserva(new BigDecimal("50.00"));
        p1.setDataVencimento(LocalDate.now().minusDays(10));
        p1.calcularValorTotal();

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));
        when(parcelaRepository.findByCotaId(cotaId)).thenReturn(List.of(p1));

        // --- ACT ---
        CotaInadimplenciaResponseDTO response = service.obterInadimplenciaCota(cotaId);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(cotaId, response.cotaId());
        assertTrue(response.possuiInadimplencia());
        assertEquals(1, response.quantidadeParcelasAtrasadas());
        assertEquals(new BigDecimal("1200.00"), response.valorOriginalAtrasado());
        // Multa: 2% de 1200.00 = 24.00
        assertEquals(new BigDecimal("24.00"), response.multaAcumulada());
        // Juros: 10 dias de atraso pro-rata die = 1% / 30 * 10 * 1200 = 4.00
        assertEquals(new BigDecimal("4.00"), response.jurosAcumulados());
        // Saldo devedor: 1200 + 24 + 4 = 1228.00
        assertEquals(new BigDecimal("1228.00"), response.saldoDevedorTotal());
        assertEquals(1, response.parcelasAtrasadas().size());
    }
}