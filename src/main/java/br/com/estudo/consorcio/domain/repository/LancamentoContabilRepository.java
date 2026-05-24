package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.LancamentoContabil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LancamentoContabilRepository extends JpaRepository<LancamentoContabil, Long> {
    Page<LancamentoContabil> findByGrupoId(Long grupoId, Pageable pageable);
    Page<LancamentoContabil> findByCotaId(Long cotaId, Pageable pageable);
}
