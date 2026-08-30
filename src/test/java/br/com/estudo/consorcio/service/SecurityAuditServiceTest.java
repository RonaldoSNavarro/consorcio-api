package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.SecurityAudit;
import br.com.estudo.consorcio.repository.SecurityAuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

    @Mock
    private SecurityAuditRepository repository;

    @InjectMocks
    private SecurityAuditService securityAuditService;

    @Test
    @DisplayName("Deve persistir log de auditoria de segurança com os dados fornecidos")
    void deveRegistrarLogAssincrono() {
        securityAuditService.registrarLogAssincrono(
                "admin", "192.168.1.1", "LOGIN_SUCCESS", "AUTH", "Login via MFA efetuado com sucesso"
        );

        verify(repository).save(argThat(audit ->
                "admin".equals(audit.getUsername()) &&
                "192.168.1.1".equals(audit.getIpAddress()) &&
                "LOGIN_SUCCESS".equals(audit.getAction()) &&
                "AUTH".equals(audit.getResource()) &&
                audit.getTimestamp() != null
        ));
    }
}