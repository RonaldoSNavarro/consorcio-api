package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Lance;
import br.com.estudo.consorcio.domain.model.StatusApuracaoLance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LanceRepository extends JpaRepository<Lance, Long> {
    List<Lance> findByAssembleiaIdOrderByValorOfertaDesc(Long assembleiaId);
    Optional<Lance> findByCotaIdAndAssembleiaId(Long cotaId, Long assembleiaId);

    // Sprint 4 — PLD/FT: Buscar lances acima do threshold no período
    List<Lance> findByStatusApuracaoAndValorOfertaGreaterThanEqualAndDataOfertaBetween(
            StatusApuracaoLance status, BigDecimal valorMinimo, LocalDateTime dataInicio, LocalDateTime dataFim);

    // Sprint 4 — Estatísticas: Contagem de lances por grupo e período
    @Query("SELECT COUNT(l) FROM Lance l WHERE l.assembleia.grupo.id = :grupoId AND l.dataOferta BETWEEN :inicio AND :fim")
    long countByGrupoIdAndPeriodo(@Param("grupoId") Long grupoId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(l) FROM Lance l WHERE l.assembleia.grupo.id = :grupoId AND l.statusApuracao = :status AND l.dataOferta BETWEEN :inicio AND :fim")
    long countByGrupoIdAndStatusAndPeriodo(@Param("grupoId") Long grupoId,
                                           @Param("status") StatusApuracaoLance status,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);
}
