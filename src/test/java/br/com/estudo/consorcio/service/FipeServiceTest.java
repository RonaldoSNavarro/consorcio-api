package br.com.estudo.consorcio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FipeServiceTest {

    private FipeService fipeService;

    @BeforeEach
    void setUp() {
        fipeService = new FipeService();
    }

    @Test
    @DisplayName("Deve fazer parse correto de strings de valor monetário FIPE")
    void deveFazerParseDeValorFipe() {
        assertEquals(new BigDecimal("75490.00"), fipeService.parseValorFipe("R$ 75.490,00"));
        assertEquals(new BigDecimal("180500.50"), fipeService.parseValorFipe("R$ 180.500,50"));
        assertEquals(new BigDecimal("1200000.00"), fipeService.parseValorFipe("R$ 1.200.000,00"));
    }

    @Test
    @DisplayName("Deve retornar ZERO para valores nulos, vazios ou inválidos")
    void deveRetornarZeroParaValoresInvalidos() {
        assertEquals(BigDecimal.ZERO, fipeService.parseValorFipe(null));
        assertEquals(BigDecimal.ZERO, fipeService.parseValorFipe(""));
        assertEquals(BigDecimal.ZERO, fipeService.parseValorFipe("invalido"));
    }
}