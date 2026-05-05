package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Contemplacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContemplacaoRepository extends JpaRepository<Contemplacao, Long> {
    List<Contemplacao> findByAssembleiaId(Long assembleiaId);
}