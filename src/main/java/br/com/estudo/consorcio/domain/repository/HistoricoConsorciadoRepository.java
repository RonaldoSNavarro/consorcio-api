package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.HistoricoConsorciado;
import br.com.estudo.consorcio.domain.model.TipoInteracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoConsorciadoRepository extends JpaRepository<HistoricoConsorciado, Long> {

    List<HistoricoConsorciado> findByClienteIdOrderByDataInteracaoDesc(Long clienteId);

    List<HistoricoConsorciado> findByClienteIdAndTipoInteracaoOrderByDataInteracaoDesc(Long clienteId, TipoInteracao tipoInteracao);

    List<HistoricoConsorciado> findByCotaIdOrderByDataInteracaoDesc(Long cotaId);
}
