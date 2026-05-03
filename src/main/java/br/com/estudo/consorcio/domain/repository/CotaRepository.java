package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Cota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CotaRepository extends JpaRepository<Cota, Long> {

    List<Cota> findByClienteId(Long clienteId);

    List<Cota> findByGrupoId(Long grupoId);
}