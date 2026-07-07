package br.com.estudo.consorcio.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LanceTest {

    @Test
    @DisplayName("Deve marcar notificarSiscoaf como true para lances vencedores com valor >= 50.000,00 e tipo FIRME")
    void deveMarcarNotificarSiscoafComoTrue() {
        Lance lance = new Lance();
        lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lance.setTipo(TipoLance.FIRME);
        lance.setValorOferta(new BigDecimal("50000.00"));

        lance.onCreate();
        assertTrue(lance.isNotificarSiscoaf());

        lance.setValorOferta(new BigDecimal("50000.01"));
        lance.onUpdate();
        assertTrue(lance.isNotificarSiscoaf());
    }

    @Test
    @DisplayName("Deve marcar notificarSiscoaf como false se o valor for menor que 50.000,00")
    void deveMarcarNotificarSiscoafComoFalseSeValorMenor() {
        Lance lance = new Lance();
        lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lance.setTipo(TipoLance.FIRME);
        lance.setValorOferta(new BigDecimal("49999.99"));

        lance.onCreate();
        assertFalse(lance.isNotificarSiscoaf());
    }

    @Test
    @DisplayName("Deve marcar notificarSiscoaf como false se o tipo nao for FIRME")
    void deveMarcarNotificarSiscoafComoFalseSeTipoDiferente() {
        Lance lance = new Lance();
        lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lance.setTipo(TipoLance.EMBUTIDO);
        lance.setValorOferta(new BigDecimal("60000.00"));

        lance.onCreate();
        assertFalse(lance.isNotificarSiscoaf());
    }

    @Test
    @DisplayName("Deve marcar notificarSiscoaf como false se o status nao for VENCEDOR")
    void deveMarcarNotificarSiscoafComoFalseSeStatusDiferente() {
        Lance lance = new Lance();
        lance.setStatusApuracao(StatusApuracaoLance.CADASTRADO);
        lance.setTipo(TipoLance.FIRME);
        lance.setValorOferta(new BigDecimal("60000.00"));

        lance.onCreate();
        assertFalse(lance.isNotificarSiscoaf());
    }
}
