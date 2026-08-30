package br.com.estudo.consorcio.config.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservabilityTest {

    @Test
    @DisplayName("Deve mascarar dados de CPF e Email para conformidade LGPD")
    void deveMascararDadosSensiveis() {
        String cpfMascarado = SecurityLogSanitizer.maskCpf("12345678900");
        assertEquals("123.***.***-00", cpfMascarado);

        String emailMascarado = SecurityLogSanitizer.maskEmail("usuario.teste@banco.com.br");
        assertTrue(emailMascarado.startsWith("us***@"));
    }

    @Test
    @DisplayName("Deve retornar health checks UP para integracoes regulatórias")
    void deveRetornarHealthUp() {
        BacenApiHealthIndicator bacen = new BacenApiHealthIndicator();
        HealthCheckResult bacenHealth = bacen.check();
        assertEquals(HealthStatus.UP, bacenHealth.status());

        SmtpHealthIndicator smtp = new SmtpHealthIndicator();
        HealthCheckResult smtpHealth = smtp.check();
        assertEquals(HealthStatus.UP, smtpHealth.status());
    }
}