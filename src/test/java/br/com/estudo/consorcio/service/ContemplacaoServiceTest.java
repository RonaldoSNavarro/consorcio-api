package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContemplacaoServiceTest {

    @Mock
    private ContemplacaoRepository contemplacaoRepository;

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @InjectMocks
    private ContemplacaoService service;

    @Test
    @DisplayName("Deve barrar e lançar exceção quando o lance embutido for maior que 30% do crédito do grupo")
    void deveBarrarLanceEmbutidoAcimaDeTrintaPorcento() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;

        // 1. Criamos um grupo com crédito de 100 mil
        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));

        // 2. Vinculamos a assembleia ao grupo
        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);

        // 3. Vinculamos a cota ao mesmo grupo
        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        // 4. Simulamos um request maldoso: Lance embutido de 35 mil (35%)
        BigDecimal lanceExcessivo = new BigDecimal("35000.00");
        ContemplacaoRequestDTO requestMalicioso = new ContemplacaoRequestDTO(
                idCota,
                idAssembleia,
                TipoContemplacao.LANCE_FIXO,
                lanceExcessivo,
                true // Lance Embutido = TRUE
        );

        // 5. Ensinamos o Mockito a devolver nossos objetos quando procurados
        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));

        // --- ACT & ASSERT ---
        // Aqui testamos se a nossa exceção realmente vai "estourar"
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.registrar(requestMalicioso);
        });

        // Verificamos se a mensagem de erro é a correta do BCB
        assertTrue(exception.getMessage().contains("não pode ultrapassar 30% do crédito"));

        // O MAIS IMPORTANTE: Garantimos que o banco de dados NUNCA foi chamado para salvar isso
        verify(contemplacaoRepository, never()).save(any());
        verify(parcelaRepository, never()).somarFundoComumPorGrupoEStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Deve barrar contemplação se o saldo do Fundo Comum for insuficiente")
    void deveBarrarContemplacaoPorSaldoInsuficiente() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;
        Long idGrupo = 10L;

        // 1. Grupo com crédito de 50.000
        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        // Pedido de contemplação normal (Sorteio, sem lance)
        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota,
                idAssembleia,
                TipoContemplacao.SORTEIO,
                null,
                false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));

        // A MÁGICA ACONTECE AQUI: Simulamos que o grupo só tem 10.000 em caixa (insuficiente para os 50k)
        when(parcelaRepository.somarFundoComumPorGrupoEStatus(idGrupo, StatusParcela.PAGA))
                .thenReturn(new BigDecimal("10000.00"));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.registrar(request);
        });

        // Verifica se a mensagem avisa sobre o saldo insuficiente
        assertTrue(exception.getMessage().contains("Saldo insuficiente no Fundo Comum"));

        // Garante que a contemplação não foi salva
        verify(contemplacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve realizar contemplação com sucesso se todas as regras forem cumpridas")
    void deveContemplarComSucesso() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;
        Long idGrupo = 10L;

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota, idAssembleia, TipoContemplacao.SORTEIO, null, false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));

        // Simulamos que o grupo tem dinheiro de sobra (100.000 em caixa)
        when(parcelaRepository.somarFundoComumPorGrupoEStatus(idGrupo, StatusParcela.PAGA))
                .thenReturn(new BigDecimal("100000.00"));

        // Ensina o mock a devolver o próprio objeto ao guardar
        when(contemplacaoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // --- ACT ---
        var resultado = service.registrar(request);

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(new BigDecimal("50000.00"), resultado.valorCreditoLiberado());
        assertEquals(StatusCota.CONTEMPLADA, cota.getStatus()); // Verifica se a cota mudou de estado
        verify(contemplacaoRepository, times(1)).save(any());
    }
}