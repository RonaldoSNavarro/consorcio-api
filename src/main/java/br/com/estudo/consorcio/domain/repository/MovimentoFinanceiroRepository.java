package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.MovimentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, Long> {

    List<MovimentoFinanceiro> findByGrupoIdOrderByDataMovimentoDesc(Long grupoId);

    List<MovimentoFinanceiro> findByCotaIdOrderByDataMovimentoDesc(Long cotaId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN m.natureza = 'CREDITO' THEN m.valor
                 WHEN m.natureza = 'DEBITO' THEN -m.valor
                 ELSE 0.0
            END
        ), 0.0)
        FROM MovimentoFinanceiro m
        WHERE m.grupo.id = :grupoId
    """)
    BigDecimal calcularSaldoGrupo(@Param("grupoId") Long grupoId);

    Optional<MovimentoFinanceiro> findFirstByGrupoIdOrderByIdDesc(Long grupoId);
}
