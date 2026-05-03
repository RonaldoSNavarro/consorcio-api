package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    Optional<Grupo> findByCodigo(String codigo);
}