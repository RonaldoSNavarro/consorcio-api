package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import br.com.estudo.consorcio.domain.model.PagamentoBancario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoBancarioRepository extends JpaRepository<PagamentoBancario, Long> {
    Optional<PagamentoBancario> findByTxid(String txid);
    List<PagamentoBancario> findByCotaIdOrderByCreatedAtDesc(Long cotaId);
    List<PagamentoBancario> findByStatus(StatusPagamentoBancario status);
}