package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.AnaliseCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseCreditoRepository extends JpaRepository<AnaliseCredito, Long> {
    List<AnaliseCredito> findByCotaId(Long cotaId);
    Optional<AnaliseCredito> findTopByCotaIdOrderByDataAnaliseDesc(Long cotaId);
}
