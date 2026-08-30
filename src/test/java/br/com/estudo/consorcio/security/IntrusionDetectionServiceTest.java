package br.com.estudo.consorcio.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntrusionDetectionServiceTest {

    private IntrusionDetectionService service;

    @BeforeEach
    void setUp() {
        service = new IntrusionDetectionService();
    }

    @Test
    @DisplayName("Deve ignorar localhost/loopback sem classificar como suspeito")
    void deveIgnorarLocalhost() {
        assertFalse(service.isSuspicious("127.0.0.1"));
        assertFalse(service.isSuspicious("0:0:0:0:0:0:0:1"));
        assertFalse(service.isSuspicious(null));
        assertFalse(service.isSuspicious("   "));
    }

    @Test
    @DisplayName("Não deve ser suspeito para tráfego dentro do limite de 30 requisições")
    void naoDeveSerSuspeitoAbaixoDoLimite() {
        String ip = "192.168.1.50";
        for (int i = 0; i < 30; i++) {
            assertFalse(service.isSuspicious(ip), "Requisição " + i + " não deveria ser suspeita");
        }
    }

    @Test
    @DisplayName("Deve classificar como suspeito quando ultrapassar 30 requisições por minuto")
    void deveClassificarComoSuspeitoAcimaDoLimite() {
        String ip = "192.168.1.55";
        for (int i = 0; i < 30; i++) {
            service.isSuspicious(ip);
        }
        assertTrue(service.isSuspicious(ip), "31ª requisição deve ser classificada como suspeita");
        assertTrue(service.isSuspicious(ip));
    }
}