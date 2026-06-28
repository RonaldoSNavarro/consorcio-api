package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Assembleia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssembleiaRepository extends JpaRepository<Assembleia, Long> {

    // Método para buscar todas as assembleias de um grupo específico
    List<Assembleia> findByGrupoId(Long grupoId);

    // Buscar ordenado por data
    List<Assembleia> findByGrupoIdOrderByDataAssembleiaAsc(Long grupoId);
}