package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.HistoricoVersaoCota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoVersaoCotaRepository extends JpaRepository<HistoricoVersaoCota, Long> {

    List<HistoricoVersaoCota> findByCotaIdOrderByDataTransicaoDesc(Long cotaId);
}
