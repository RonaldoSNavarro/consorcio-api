package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaComplianceRepository extends JpaRepository<AlertaCompliance, Long> {
    List<AlertaCompliance> findByStatus(StatusAlertaCompliance status);
    boolean existsByClienteIdAndListaRestritivaId(Long clienteId, Long listaId);
    boolean existsByClienteIdAndStatusIn(Long clienteId, List<StatusAlertaCompliance> statuses);
    List<AlertaCompliance> findByClienteIdAndStatusIn(Long clienteId, List<StatusAlertaCompliance> statuses);
    List<AlertaCompliance> findByClienteIdInAndStatusIn(List<Long> clienteIds, List<StatusAlertaCompliance> statuses);

    interface MatchResultProjection {
        Long getClienteId();
        Long getListaId();
        Double getScore();
    }

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT c.id as clienteId, lr.id as listaId, similarity(UPPER(c.nome), UPPER(lr.nome)) as score
        FROM clientes c
        CROSS JOIN listas_restritivas lr
        WHERE lr.origem IN ('OFAC', 'ONU')
          AND UPPER(c.nome) % UPPER(lr.nome)
          AND similarity(UPPER(c.nome), UPPER(lr.nome)) >= :threshold
          AND NOT EXISTS (SELECT 1 FROM alertas_compliance ac WHERE ac.cliente_id = c.id AND ac.lista_id = lr.id)
    """)
    List<MatchResultProjection> findOfacOnuMatches(@org.springframework.data.repository.query.Param("threshold") Double threshold);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT c.id as clienteId, lr.id as listaId, similarity(UPPER(c.nome), UPPER(lr.nome)) as score
        FROM clientes c
        JOIN listas_restritivas lr 
          ON lr.origem = 'PEP' 
          AND SUBSTRING(REGEXP_REPLACE(lr.documento_origem, '[^0-9]', '', 'g') FROM 4 FOR 6) = SUBSTRING(REGEXP_REPLACE(c.cpf_cnpj, '[^0-9]', '', 'g') FROM 4 FOR 6)
        WHERE UPPER(c.nome) % UPPER(lr.nome)
          AND similarity(UPPER(c.nome), UPPER(lr.nome)) >= :threshold
          AND NOT EXISTS (SELECT 1 FROM alertas_compliance ac WHERE ac.cliente_id = c.id AND ac.lista_id = lr.id)
    """)
    List<MatchResultProjection> findPepMatches(@org.springframework.data.repository.query.Param("threshold") Double threshold);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT c.id as clienteId, lr.id as listaId, 1.0 as score
        FROM clientes c
        JOIN listas_restritivas lr 
          ON lr.origem = 'IBGE' 
          AND UPPER(TRIM(c.localidade) || ' - ' || TRIM(c.uf)) = UPPER(TRIM(lr.nome))
        WHERE NOT EXISTS (SELECT 1 FROM alertas_compliance ac WHERE ac.cliente_id = c.id AND ac.lista_id = lr.id)
    """)
    List<MatchResultProjection> findIbgeMatches();
}
