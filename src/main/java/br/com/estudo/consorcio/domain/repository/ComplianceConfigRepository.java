package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ComplianceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplianceConfigRepository extends JpaRepository<ComplianceConfig, Long> {
}
