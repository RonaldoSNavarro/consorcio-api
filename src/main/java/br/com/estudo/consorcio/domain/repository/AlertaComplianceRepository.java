package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaComplianceRepository extends JpaRepository<AlertaCompliance, Long> {
    List<AlertaCompliance> findByStatus(StatusAlertaCompliance status);
}
