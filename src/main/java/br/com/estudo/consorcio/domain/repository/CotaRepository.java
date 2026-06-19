package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.StatusCota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CotaRepository extends JpaRepository<Cota, Long> {

    Page<Cota> findByClienteId(Long clienteId, Pageable pageable);

    Page<Cota> findByGrupoId(Long grupoId, Pageable pageable);
    
    // For internal processing that still needs List
    List<Cota> findByGrupoId(Long grupoId);

    // Sprint 4 — Estatísticas: Contagem por grupo e status
    long countByGrupoIdAndStatus(Long grupoId, StatusCota status);

    // Sprint 4 — Contagem total de cotas no grupo
    long countByGrupoId(Long grupoId);

    // Relatórios e PLD/FT: Contagem de cotas inadimplentes (> 3 parcelas em atraso)
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Cota c WHERE c.grupo.id = :grupoId AND (SELECT COUNT(p) FROM Parcela p WHERE p.cota = c AND p.status = :statusAtrasada) > 3")
    long countCotasInadimplentes(@org.springframework.data.repository.query.Param("grupoId") Long grupoId, @org.springframework.data.repository.query.Param("statusAtrasada") br.com.estudo.consorcio.domain.model.StatusParcela statusAtrasada);
}