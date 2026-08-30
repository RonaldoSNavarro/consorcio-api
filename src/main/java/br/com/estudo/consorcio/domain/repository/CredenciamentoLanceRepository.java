package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.CredenciamentoLance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredenciamentoLanceRepository extends JpaRepository<CredenciamentoLance, Long> {
    List<CredenciamentoLance> findByAssembleiaIdAndStatus(Long assembleiaId, StatusCredenciamento status);
    Optional<CredenciamentoLance> findByCotaIdAndAssembleiaIdAndStatus(Long cotaId, Long assembleiaId, StatusCredenciamento status);
    List<CredenciamentoLance> findByCotaIdOrderByCreatedAtDesc(Long cotaId);
}