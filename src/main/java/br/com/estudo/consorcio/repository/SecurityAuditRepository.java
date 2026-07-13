package br.com.estudo.consorcio.repository;

import br.com.estudo.consorcio.domain.model.SecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
}
