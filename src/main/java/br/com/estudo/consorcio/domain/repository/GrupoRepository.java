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
}