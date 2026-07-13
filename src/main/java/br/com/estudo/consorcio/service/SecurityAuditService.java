package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.SecurityAudit;
import br.com.estudo.consorcio.repository.SecurityAuditRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SecurityAuditService {

    private final SecurityAuditRepository repository;

    public SecurityAuditService(SecurityAuditRepository repository) {
        this.repository = repository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLogAssincrono(String username, String ipAddress, String action, String resource, String details) {
        SecurityAudit audit = new SecurityAudit(LocalDateTime.now(), username, ipAddress, action, resource, details);
        repository.save(audit);
    }
}
