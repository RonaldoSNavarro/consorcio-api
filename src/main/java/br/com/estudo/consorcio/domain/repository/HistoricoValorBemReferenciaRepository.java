package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.HistoricoValorBemReferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoValorBemReferenciaRepository extends JpaRepository<HistoricoValorBemReferencia, Long> {
    List<HistoricoValorBemReferencia> findByBemReferenciaIdOrderByDataAtualizacaoDesc(Long bemReferenciaId);
}
