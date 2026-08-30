package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Corretor;
import br.com.estudo.consorcio.domain.repository.CorretorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorretorServiceTest {

    @Mock
    private CorretorRepository corretorRepository;

    @InjectMocks
    private CorretorService corretorService;

    @Test
    @DisplayName("Deve acumular dívida de clawback no saldo devedor do corretor")
    void deveAdicionarDividaClawback() {
        Corretor corretor = new Corretor();
        corretor.setId(1L);
        corretor.setSaldoDevedor(new BigDecimal("100.00"));

        corretorService.adicionarDividaClawback(corretor, new BigDecimal("250.00"));

        assertEquals(new BigDecimal("350.00"), corretor.getSaldoDevedor());
        verify(corretorRepository).save(corretor);
    }

    @Test
    @DisplayName("Deve inicializar saldo devedor se for nulo ao adicionar dívida")
    void deveInicializarSaldoDevedorNulo() {
        Corretor corretor = new Corretor();
        corretor.setId(2L);
        corretor.setSaldoDevedor(null);

        corretorService.adicionarDividaClawback(corretor, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("500.00"), corretor.getSaldoDevedor());
        verify(corretorRepository).save(corretor);
    }

    @Test
    @DisplayName("Não deve executar ação se corretor ou valor forem nulos")
    void naoDeveExecutarComParametrosNulos() {
        corretorService.adicionarDividaClawback(null, new BigDecimal("100.00"));
        corretorService.adicionarDividaClawback(new Corretor(), null);

        verifyNoInteractions(corretorRepository);
    }
}