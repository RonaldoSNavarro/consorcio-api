package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.IndiceEconomicoDTO;
import br.com.estudo.consorcio.domain.dto.SimulacaoReajusteResponseDTO;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.domain.repository.IndiceEconomicoRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BcbSgsServiceTest {

    @Mock
    private IndiceEconomicoRepository repository;

    @InjectMocks
    private BcbSgsService bcbSgsService;

    @Test
    @DisplayName("Deve retornar o código de série do BACEN correto para cada índice")
    void deveRetornarCodigoSerieCorreto() {
        assertEquals("433", bcbSgsService.obterCodigoSerieBcb(IndiceReajuste.IPCA));
        assertEquals("192", bcbSgsService.obterCodigoSerieBcb(IndiceReajuste.INCC));
        assertEquals("189", bcbSgsService.obterCodigoSerieBcb(IndiceReajuste.IGP_M));
        assertNull(bcbSgsService.obterCodigoSerieBcb(IndiceReajuste.MANUAL));
        assertNull(bcbSgsService.obterCodigoSerieBcb(null));
    }

    @Test
    @DisplayName("Deve calcular taxa acumulada de 12 meses corretamente")
    void deveCalcularAcumulado12Meses() {
        List<IndiceEconomicoDTO> indices = List.of(
                new IndiceEconomicoDTO(IndiceReajuste.IPCA, LocalDate.now().minusMonths(2), new BigDecimal("0.50")),
                new IndiceEconomicoDTO(IndiceReajuste.IPCA, LocalDate.now().minusMonths(1), new BigDecimal("0.30"))
        );

        BigDecimal acumulado = bcbSgsService.calcularAcumulado12Meses(indices);

        assertEquals(new BigDecimal("0.8015"), acumulado);
    }

    @Test
    @DisplayName("Deve retornar ZERO se a lista de índices para acumulado for vazia")
    void deveRetornarZeroSemIndices() {
        assertEquals(BigDecimal.ZERO, bcbSgsService.calcularAcumulado12Meses(Collections.emptyList()));
        assertEquals(BigDecimal.ZERO, bcbSgsService.calcularAcumulado12Meses(null));
    }

    @Test
    @DisplayName("Deve simular reajuste com sucesso aplicando o fator de reajuste")
    void deveSimularReajusteComSucesso() {
        when(repository.findTop12ByTipoIndiceOrderByDataReferenciaDesc(any())).thenReturn(Collections.emptyList());

        BigDecimal valorAtual = new BigDecimal("100000.00");
        SimulacaoReajusteResponseDTO sim = bcbSgsService.simularReajuste(IndiceReajuste.IPCA, valorAtual);

        assertNotNull(sim);
        assertEquals(valorAtual, sim.valorOriginal());
        assertEquals(valorAtual, sim.novoValorCalculado());
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar simular com valor nulo ou zero")
    void deveLancarErroSimulacaoValorInvalido() {
        assertThrows(RegraDeNegocioException.class, () ->
                bcbSgsService.simularReajuste(IndiceReajuste.IPCA, BigDecimal.ZERO));
        assertThrows(RegraDeNegocioException.class, () ->
                bcbSgsService.simularReajuste(IndiceReajuste.IPCA, null));
    }
}