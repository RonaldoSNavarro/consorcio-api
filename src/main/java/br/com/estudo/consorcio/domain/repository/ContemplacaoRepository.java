package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Contemplacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContemplacaoRepository extends JpaRepository<Contemplacao, Long> {
    List<Contemplacao> findByAssembleiaId(Long assembleiaId);

    // Módulo 2: Soma todos os créditos liberados nas contemplações do grupo
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.valorCreditoLiberado), 0) FROM Contemplacao c WHERE c.assembleia.grupo.id = :grupoId")
    java.math.BigDecimal somarCreditosLiberadosPorGrupo(@org.springframework.data.repository.query.Param("grupoId") Long grupoId);
}