package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Cota;
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
}