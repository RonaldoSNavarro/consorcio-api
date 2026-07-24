package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.BemReferencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BemReferenciaRepository extends JpaRepository<BemReferencia, Long> {
    List<BemReferencia> findByCategoriaBemId(Long categoriaBemId);
    Page<BemReferencia> findByCategoriaBemId(Long categoriaBemId, Pageable pageable);
    List<BemReferencia> findByAtivoTrue();
}

