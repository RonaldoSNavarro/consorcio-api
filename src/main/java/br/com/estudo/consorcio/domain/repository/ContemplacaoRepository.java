package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Contemplacao;
import br.com.estudo.consorcio.domain.model.TipoContemplacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContemplacaoRepository extends JpaRepository<Contemplacao, Long> {
    List<Contemplacao> findByAssembleiaId(Long assembleiaId);
    java.util.Optional<Contemplacao> findTopByCotaIdOrderByDataContemplacaoDesc(Long cotaId);

    // Módulo 2: Soma todos os créditos liberados nas contemplações do grupo
    @Query("SELECT COALESCE(SUM(c.valorCreditoLiberado), 0) FROM Contemplacao c WHERE c.assembleia.grupo.id = :grupoId")
    BigDecimal somarCreditosLiberadosPorGrupo(@Param("grupoId") Long grupoId);

    // Sprint 4 — Estatísticas: Contagem por grupo, tipo e período
    @Query("SELECT COUNT(c) FROM Contemplacao c WHERE c.assembleia.grupo.id = :grupoId AND c.tipoContemplacao = :tipo AND c.dataContemplacao BETWEEN :inicio AND :fim")
    long countByGrupoIdAndTipoAndPeriodo(@Param("grupoId") Long grupoId,
                                          @Param("tipo") TipoContemplacao tipo,
                                          @Param("inicio") LocalDate inicio,
                                          @Param("fim") LocalDate fim);

    // Sprint 4 — Estatísticas: Soma de créditos liberados no período
    @Query("SELECT COALESCE(SUM(c.valorCreditoLiberado), 0) FROM Contemplacao c WHERE c.assembleia.grupo.id = :grupoId AND c.dataContemplacao BETWEEN :inicio AND :fim")
    BigDecimal somarCreditosLiberadosPorGrupoEPeriodo(@Param("grupoId") Long grupoId,
                                                       @Param("inicio") LocalDate inicio,
                                                       @Param("fim") LocalDate fim);
}