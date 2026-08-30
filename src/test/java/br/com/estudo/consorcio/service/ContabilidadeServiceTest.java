package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.repository.ContaContabilRepository;
import br.com.estudo.consorcio.domain.repository.LancamentoContabilRepository;
import br.com.estudo.consorcio.domain.model.ContaContabil;
import br.com.estudo.consorcio.domain.model.NaturezaContabil;
import br.com.estudo.consorcio.domain.model.TipoContaContabil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContabilidadeServiceTest {

    @Test
    void registrarBaixa_deveCriarContaOperacional_quandoContaCosifNaoExiste() {
        ContaContabilRepository contaRepository = mock(ContaContabilRepository.class);
        when(contaRepository.findByCodigoCosif(ContabilidadeService.CONTA_CAIXA)).thenReturn(Optional.empty());
        when(contaRepository.save(any(ContaContabil.class))).thenAnswer(invocation -> {
            ContaContabil conta = invocation.getArgument(0);
            conta.setTipo(TipoContaContabil.ATIVO);
            conta.setNatureza(NaturezaContabil.DEVEDORA);
            return conta;
        });
        ContabilidadeService service = new ContabilidadeService(mock(LancamentoContabilRepository.class), contaRepository);

        assertDoesNotThrow(() -> service.registrarBaixa(
                null, null, null,
                ContabilidadeService.CONTA_CAIXA,
                ContabilidadeService.CONTA_FUNDO_COMUM,
                BigDecimal.ONE,
                LocalDate.now(),
                "Teste de conta ausente"));
    }

    @Test
    void provisionarPlanoContasOperacional_deveCriarDozeContasUmaUnicaVez() {
        ContaContabilRepository contaRepository = mock(ContaContabilRepository.class);
        Map<String, ContaContabil> contasPersistidas = new HashMap<>();
        when(contaRepository.findByCodigoCosif(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(contasPersistidas.get(invocation.getArgument(0))));
        when(contaRepository.save(any(ContaContabil.class))).thenAnswer(invocation -> {
            ContaContabil conta = invocation.getArgument(0);
            contasPersistidas.put(conta.getCodigoCosif(), conta);
            return conta;
        });
        ContabilidadeService service = new ContabilidadeService(mock(LancamentoContabilRepository.class), contaRepository);

        int criadasNaPrimeiraExecucao = service.provisionarPlanoContasOperacional();
        int criadasNaSegundaExecucao = service.provisionarPlanoContasOperacional();

        assertEquals(12, criadasNaPrimeiraExecucao);
        assertEquals(0, criadasNaSegundaExecucao);
        assertEquals(12, contasPersistidas.size());
        verify(contaRepository, times(12)).save(any(ContaContabil.class));
    }
}
