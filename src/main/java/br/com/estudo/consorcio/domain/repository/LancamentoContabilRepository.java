package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.LancamentoContabil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface LancamentoContabilRepository extends JpaRepository<LancamentoContabil, Long> {
    Page<LancamentoContabil> findByGrupoId(Long grupoId, Pageable pageable);
    Page<LancamentoContabil> findByCotaId(Long cotaId, Pageable pageable);

    // FC-05 FIX: Queries agregadas executadas no PostgreSQL — evita carregar milhões de registros em memória
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoContabil l WHERE l.grupo.id = :grupoId AND l.contaCredito.id = :contaId")
    BigDecimal somarCreditosPorGrupoEConta(@Param("grupoId") Long grupoId, @Param("contaId") Long contaId);

    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoContabil l WHERE l.grupo.id = :grupoId AND l.contaDebito.id = :contaId")
    BigDecimal somarDebitosPorGrupoEConta(@Param("grupoId") Long grupoId, @Param("contaId") Long contaId);
}
