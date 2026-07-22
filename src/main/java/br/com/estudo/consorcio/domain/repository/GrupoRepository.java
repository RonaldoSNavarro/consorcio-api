package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    Optional<Grupo> findByCodigoGrupo(String codigoGrupo);
    
    @Deprecated
    default Optional<Grupo> findByCodigo(String codigo) {
        return findByCodigoGrupo(codigo);
    }
    
    List<Grupo> findByStatusAndDataEncerramentoBefore(StatusGrupo status, LocalDate date);
    
    @org.springframework.data.jpa.repository.Query("""
        SELECT new br.com.estudo.consorcio.domain.dto.GrupoFinanceiroDTO(
            g.id, g.codigoGrupo,
            COALESCE(SUM(p.valorFundoComum), 0),
            COALESCE(SUM(p.valorTaxaAdministracao), 0),
            COALESCE(SUM(p.valorFundoReserva), 0)
        )
        FROM Grupo g
        LEFT JOIN Cota c ON c.grupo.id = g.id
        LEFT JOIN Parcela p ON p.cota.id = c.id AND p.status = 'PAGA'
        GROUP BY g.id, g.codigoGrupo
    """)
    List<br.com.estudo.consorcio.domain.dto.GrupoFinanceiroDTO> findGruposFinanceiroResumo();

    @org.springframework.data.jpa.repository.Query("""
        SELECT g FROM Grupo g WHERE g.categoriaBem = :categoria
        AND (g.status = 'EM_ANDAMENTO' OR g.status = 'EM_FORMACAO')
        AND (SELECT COUNT(c) FROM Cota c WHERE c.grupo = g) < 100
        ORDER BY g.status ASC, (SELECT COUNT(c) FROM Cota c WHERE c.grupo = g) DESC
        LIMIT 1
    """)
    Optional<Grupo> encontrarMelhorGrupoDisponivel(@org.springframework.data.repository.query.Param("categoria") br.com.estudo.consorcio.domain.enums.CategoriaBem categoria);
}