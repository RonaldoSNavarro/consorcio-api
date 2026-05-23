package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.MovimentoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.MovimentoFinanceiroRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimentoFinanceiroServiceTest {

    @Mock
    private MovimentoFinanceiroRepository repository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.MovimentoFinanceiroMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.MovimentoFinanceiroMapper.class);

    @InjectMocks
    private MovimentoFinanceiroService service;

    @Test
    @DisplayName("Deve registrar movimento de CREDITO calculando os saldos corretamente")
    void deveRegistrarMovimentoDeCreditoCalculandoSaldos() {
        // --- ARRANGE ---
        Grupo grupo = new Grupo();
        grupo.setId(1L);

        Cota cota = new Cota();
        cota.setId(10L);

        // Simulamos que já existe um último movimento com saldo de R$ 500,00
        MovimentoFinanceiro ultimoMovimento = MovimentoFinanceiro.builder()
                .id(99L)
                .saldoPosterior(new BigDecimal("500.00"))
                .build();

        when(repository.findFirstByGrupoIdOrderByIdDesc(grupo.getId()))
                .thenReturn(Optional.of(ultimoMovimento));

        when(repository.save(any(MovimentoFinanceiro.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        MovimentoFinanceiro resultado = service.registrarMovimento(
                grupo, cota, null, null,
                TipoMovimentoFinanceiro.FUNDO_COMUM, NaturezaMovimento.CREDITO,
                new BigDecimal("150.00"), "Pagamento Fundo Comum", null
        );

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(new BigDecimal("500.00"), resultado.getSaldoAnterior());
        assertEquals(new BigDecimal("650.00"), resultado.getSaldoPosterior()); // 500 + 150
        assertEquals(NaturezaMovimento.CREDITO, resultado.getNatureza());
        verify(repository, times(1)).save(any(MovimentoFinanceiro.class));
    }

    @Test
    @DisplayName("Deve registrar movimento de DEBITO calculando os saldos corretamente")
    void deveRegistrarMovimentoDeDebitoCalculandoSaldos() {
        // --- ARRANGE ---
        Grupo grupo = new Grupo();
        grupo.setId(1L);

        Cota cota = new Cota();
        cota.setId(10L);

        // Simulamos que já existe um último movimento com saldo de R$ 500,00
        MovimentoFinanceiro ultimoMovimento = MovimentoFinanceiro.builder()
                .id(99L)
                .saldoPosterior(new BigDecimal("500.00"))
                .build();

        when(repository.findFirstByGrupoIdOrderByIdDesc(grupo.getId()))
                .thenReturn(Optional.of(ultimoMovimento));

        when(repository.save(any(MovimentoFinanceiro.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        MovimentoFinanceiro resultado = service.registrarMovimento(
                grupo, cota, null, null,
                TipoMovimentoFinanceiro.LIBERACAO_CREDITO, NaturezaMovimento.DEBITO,
                new BigDecimal("150.00"), "Liberacao Credito", null
        );

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(new BigDecimal("500.00"), resultado.getSaldoAnterior());
        assertEquals(new BigDecimal("350.00"), resultado.getSaldoPosterior()); // 500 - 150
        assertEquals(NaturezaMovimento.DEBITO, resultado.getNatureza());
        verify(repository, times(1)).save(any(MovimentoFinanceiro.class));
    }

    @Test
    @DisplayName("Deve registrar movimento com saldo anterior zero se for o primeiro movimento do grupo")
    void deveRegistrarMovimentoComSaldoAnteriorZero() {
        // --- ARRANGE ---
        Grupo grupo = new Grupo();
        grupo.setId(1L);

        when(repository.findFirstByGrupoIdOrderByIdDesc(grupo.getId()))
                .thenReturn(Optional.empty());

        when(repository.save(any(MovimentoFinanceiro.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        MovimentoFinanceiro resultado = service.registrarMovimento(
                grupo, null, null, null,
                TipoMovimentoFinanceiro.FUNDO_COMUM, NaturezaMovimento.CREDITO,
                new BigDecimal("100.00"), "Primeiro Movimento", null
        );

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(BigDecimal.ZERO, resultado.getSaldoAnterior());
        assertEquals(new BigDecimal("100.00"), resultado.getSaldoPosterior());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar registrar movimento com valor negativo ou nulo")
    void deveLancarExcecaoValorInvalido() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimento(
                grupo, null, null, null,
                TipoMovimentoFinanceiro.FUNDO_COMUM, NaturezaMovimento.CREDITO,
                new BigDecimal("-50.00"), "Valor Negativo", null
        ));

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimento(
                grupo, null, null, null,
                TipoMovimentoFinanceiro.FUNDO_COMUM, NaturezaMovimento.CREDITO,
                null, "Valor Nulo", null
        ));
    }

    @Test
    @DisplayName("Deve listar todos os movimentos do grupo")
    void deveListarMovimentosPorGrupo() {
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);

        MovimentoFinanceiro m1 = MovimentoFinanceiro.builder()
                .id(1L)
                .grupo(grupo)
                .tipoMovimento(TipoMovimentoFinanceiro.FUNDO_COMUM)
                .natureza(NaturezaMovimento.CREDITO)
                .valor(new BigDecimal("100.00"))
                .dataMovimento(LocalDateTime.now())
                .build();

        when(repository.findByGrupoIdOrderByDataMovimentoDesc(grupoId)).thenReturn(List.of(m1));

        List<MovimentoFinanceiroResponseDTO> resultado = service.listarPorGrupo(grupoId);

        assertEquals(1, resultado.size());
        assertEquals(new BigDecimal("100.00"), resultado.get(0).valor());
        assertEquals(TipoMovimentoFinanceiro.FUNDO_COMUM, resultado.get(0).tipoMovimento());
    }

    @Test
    @DisplayName("Deve obter saldo do grupo")
    void deveObterSaldoGrupo() {
        Long grupoId = 1L;
        when(repository.calcularSaldoGrupo(grupoId)).thenReturn(new BigDecimal("15000.00"));

        BigDecimal saldo = service.obterSaldoGrupo(grupoId);

        assertEquals(new BigDecimal("15000.00"), saldo);
        verify(repository, times(1)).calcularSaldoGrupo(grupoId);
    }
}
