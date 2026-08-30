package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import br.com.estudo.consorcio.domain.model.TransferenciaCota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaCotaRepository extends JpaRepository<TransferenciaCota, Long> {
    List<TransferenciaCota> findByCotaIdOrderByDataSolicitacaoDesc(Long cotaId);
    List<TransferenciaCota> findByStatusOrderByDataSolicitacaoAsc(StatusTransferencia status);
    List<TransferenciaCota> findByCedenteIdOrCessionarioIdOrderByDataSolicitacaoDesc(Long cedenteId, Long cessionarioId);
}