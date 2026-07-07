package br.com.estudo.consorcio.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LanceSiscoafTest {

    @ParameterizedTest
    @CsvSource({
            "VENCEDOR, FIRME, 50000.00, true",
            "VENCEDOR, FIRME, 50000.01, true",
            "VENCEDOR, FIRME, 100000.00, true",
            "VENCEDOR, FIRME, 49999.99, false",
            "VENCEDOR, EMBUTIDO, 50000.00, false", // Siscoaf is only for TipoLance.FIRME (recursos próprios)
            "VENCEDOR, MISTO, 50000.00, false", // MISTO should be false or handled differently, but FIRME is the trigger
            "CADASTRADO, FIRME, 50000.00, false", // Only winning lances
            "PERDEDOR, FIRME, 50000.00, false",
            "INVALIDO, FIRME, 50000.00, false"
    })
    @DisplayName("Teste Parametrizado - Notificação Siscoaf para Lances (Circular 3.978/2020)")
    void testNotificarSiscoaf(StatusApuracaoLance status, TipoLance tipo, String valorOfertaStr, boolean expectedNotificar) throws Exception {
        // Arrange
        Lance lance = new Lance();
        lance.setStatusApuracao(status);
        lance.setTipo(tipo);
        lance.setValorOferta(new BigDecimal(valorOfertaStr));

        // Use reflection to invoke the protected @PrePersist method
        Method onCreate = Lance.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        // Act
        onCreate.invoke(lance);

        // Assert
        assertEquals(expectedNotificar, lance.isNotificarSiscoaf(),
                "A flag notificarSiscoaf não correspondeu ao esperado para Status=" + status + ", Tipo=" + tipo + ", Valor=" + valorOfertaStr);
    }
    
    @ParameterizedTest
    @CsvSource({
            "VENCEDOR, FIRME, 50000.00, true",
            "VENCEDOR, FIRME, 49999.99, false",
            "CADASTRADO, FIRME, 50000.00, false"
    })
    @DisplayName("Teste Parametrizado - Atualização de Lance aciona regra Siscoaf via @PreUpdate")
    void testNotificarSiscoafOnUpdate(StatusApuracaoLance status, TipoLance tipo, String valorOfertaStr, boolean expectedNotificar) throws Exception {
        // Arrange
        Lance lance = new Lance();
        lance.setStatusApuracao(status);
        lance.setTipo(tipo);
        lance.setValorOferta(new BigDecimal(valorOfertaStr));

        // Use reflection to invoke the protected @PreUpdate method
        Method onUpdate = Lance.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);

        // Act
        onUpdate.invoke(lance);

        // Assert
        assertEquals(expectedNotificar, lance.isNotificarSiscoaf());
    }
}
