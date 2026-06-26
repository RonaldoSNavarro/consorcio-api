package br.com.estudo.consorcio.repository;

import br.com.estudo.consorcio.domain.model.ComplianceExecucaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceExecucaoLogRepository extends JpaRepository<ComplianceExecucaoLog, Long> {
    List<ComplianceExecucaoLog> findTop50ByOrderByDataExecucaoDesc();
}
