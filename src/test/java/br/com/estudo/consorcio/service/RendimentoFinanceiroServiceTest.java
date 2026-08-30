package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.NaturezaMovimento;
import br.com.estudo.consorcio.domain.model.RendimentoFinanceiro;
import br.com.estudo.consorcio.domain.model.TipoMovimentoFinanceiro;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.RendimentoFinanceiroRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendimentoFinanceiroServiceTest {

    @Mock
    private RendimentoFinanceiroRepository rendimentoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private MovimentoFinanceiroService movimentoService;

    @Mock
    private ContabilidadeService contabilidadeService;

    @InjectMocks
    private RendimentoFinanceiroService rendimentoService;

    private Grupo grupo;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setCodigo("GRP-001");
    }

    @Test
    @DisplayName("Deve registrar rendimento financeiro e gerar lançamentos contábeis e extrato")
    void deveRegistrarRendimentoComSucesso() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(rendimentoRepository.save(any(RendimentoFinanceiro.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal valor = new BigDecimal("450.75");
        LocalDate data = LocalDate.now();

        RendimentoFinanceiro res = rendimentoService.registrarRendimento(10L, valor, data, "Rendimento CDI mensal");

        assertNotNull(res);
        assertEquals(valor, res.getValorRendimento());
        assertEquals(grupo, res.getGrupo());

        verify(contabilidadeService).registrarBaixa(
                eq(grupo), isNull(), isNull(),
                eq(ContabilidadeService.CONTA_CAIXA),
                eq(ContabilidadeService.CONTA_FUNDO_RESERVA),
                eq(valor), eq(data), anyString()
        );

        verify(movimentoService).registrarMovimento(
                eq(grupo), isNull(), isNull(), isNull(),
                eq(TipoMovimentoFinanceiro.LANCAMENTO_MANUAL),
                eq(NaturezaMovimento.CREDITO),
                eq(valor), anyString(), any()
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar registrar rendimento com valor zero ou negativo")
    void deveLancarExcecaoValorInvalido() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        assertThrows(RegraDeNegocioException.class, () ->
                rendimentoService.registrarRendimento(10L, BigDecimal.ZERO, LocalDate.now(), "Invalido"));

        assertThrows(RegraDeNegocioException.class, () ->
                rendimentoService.registrarRendimento(10L, new BigDecimal("-50.00"), LocalDate.now(), "Invalido"));
    }

    @Test
    @DisplayName("Deve lançar exceção se o grupo não existir")
    void deveLancarExcecaoGrupoNaoEncontrado() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class, () ->
                rendimentoService.registrarRendimento(99L, new BigDecimal("100.00"), LocalDate.now(), "Teste"));
    }
}