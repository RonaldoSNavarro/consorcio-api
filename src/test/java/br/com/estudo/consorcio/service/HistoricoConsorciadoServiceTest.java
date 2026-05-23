package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.HistoricoConsorciadoResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.HistoricoConsorciadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricoConsorciadoServiceTest {

    @Mock
    private HistoricoConsorciadoRepository repository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.HistoricoConsorciadoMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.HistoricoConsorciadoMapper.class);

    @InjectMocks
    private HistoricoConsorciadoService service;

    @Test
    @DisplayName("Deve calcular valor da categoria corretamente conforme a fórmula de 4 componentes")
    void deveCalcularValorCategoriaCorretamente() {
        // --- ARRANGE ---
        // Exemplo da regra de negócio documentada:
        // Crédito R$ 120.000, prazo 60 meses:
        // - Fundo Comum (Crédito / Prazo): 120.000 / 60 = R$ 2.000,00
        // - Taxa Adm: R$ 300,00
        // - Fundo Reserva: R$ 100,00
        // - Seguro: R$ 50,00
        // - Parcela base = R$ 2.450,00
        // - Valor Categoria = R$ 2.450 * 60 = R$ 147.000,00
        BigDecimal fc = new BigDecimal("2000.00");
        BigDecimal taxa = new BigDecimal("300.00");
        BigDecimal reserva = new BigDecimal("100.00");
        BigDecimal seguro = new BigDecimal("50.00");
        int prazo = 60;

        // --- ACT ---
        BigDecimal resultado = service.calcularValorCategoria(fc, taxa, reserva, seguro, prazo);

        // --- ASSERT ---
        assertEquals(new BigDecimal("147000.00"), resultado);
    }

    @Test
    @DisplayName("Deve retornar valor da categoria como ZERO se algum componente for nulo")
    void deveRetornarZeroSeComponenteForNulo() {
        BigDecimal fc = new BigDecimal("2000.00");
        BigDecimal taxa = new BigDecimal("300.00");

        BigDecimal resultado = service.calcularValorCategoria(fc, taxa, null, null, 60);

        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    @DisplayName("Deve registrar interação de histórico calculando valor_categoria e preenchendo o snapshot")
    void deveRegistrarInteracaoComSucesso() {
        // --- ARRANGE ---
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Cota cota = new Cota();
        cota.setId(10L);

        Grupo grupo = new Grupo();
        grupo.setId(100L);
        grupo.setPrazoMeses(60);

        BigDecimal fc = new BigDecimal("2000.00");
        BigDecimal taxa = new BigDecimal("300.00");
        BigDecimal reserva = new BigDecimal("100.00");
        BigDecimal seguro = new BigDecimal("50.00");

        when(repository.save(any(HistoricoConsorciado.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        HistoricoConsorciado resultado = service.registrarInteracao(
                cliente, cota, grupo, null,
                TipoInteracao.PAGAMENTO_PARCELA, "Pagamento efetuado",
                new BigDecimal("120000.00"), fc, taxa, reserva, seguro,
                null, null, null
        );

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(cliente, resultado.getCliente());
        assertEquals(cota, resultado.getCota());
        assertEquals(grupo, resultado.getGrupo());
        assertEquals(TipoInteracao.PAGAMENTO_PARCELA, resultado.getTipoInteracao());
        // Fundo Comum (2000) + Taxa (300) + Reserva (100) + Seguro (50) = 2450 * 60 = 147000
        assertEquals(new BigDecimal("147000.00"), resultado.getValorCategoria());
        verify(repository, times(1)).save(any(HistoricoConsorciado.class));
    }

    @Test
    @DisplayName("Deve listar interações por ID do cliente")
    void deveListarInteracoesPorCliente() {
        // --- ARRANGE ---
        Long clienteId = 1L;
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        HistoricoConsorciado h1 = HistoricoConsorciado.builder()
                .id(1L)
                .cliente(cliente)
                .tipoInteracao(TipoInteracao.ATUALIZACAO_CADASTRAL)
                .descricao("Atualizou cep")
                .dataInteracao(LocalDateTime.now())
                .build();

        when(repository.findByClienteIdOrderByDataInteracaoDesc(clienteId)).thenReturn(List.of(h1));

        // --- ACT ---
        List<HistoricoConsorciadoResponseDTO> resultado = service.listarPorCliente(clienteId);

        // --- ASSERT ---
        assertEquals(1, resultado.size());
        assertEquals(TipoInteracao.ATUALIZACAO_CADASTRAL, resultado.get(0).tipoInteracao());
        assertEquals("Atualizou cep", resultado.get(0).descricao());
    }
}
