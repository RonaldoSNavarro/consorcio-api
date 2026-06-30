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

    Optional<Grupo> findByCodigo(String codigo);
    
    List<Grupo> findByStatusAndDataEncerramentoBefore(StatusGrupo status, LocalDate date);
    
    @org.springframework.data.jpa.repository.Query("""
        SELECT new br.com.estudo.consorcio.domain.dto.GrupoFinanceiroDTO(
            g.id, g.codigo,
            COALESCE(SUM(p.valorFundoComum), 0),
            COALESCE(SUM(p.valorTaxaAdministracao), 0),
            COALESCE(SUM(p.valorFundoReserva), 0)
        )
        FROM Grupo g
        LEFT JOIN Cota c ON c.grupo.id = g.id
        LEFT JOIN Parcela p ON p.cota.id = c.id AND p.status = 'PAGA'
        GROUP BY g.id, g.codigo
    """)
    List<br.com.estudo.consorcio.domain.dto.GrupoFinanceiroDTO> findGruposFinanceiroResumo();
}