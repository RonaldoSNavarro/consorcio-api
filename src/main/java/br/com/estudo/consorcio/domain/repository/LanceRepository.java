package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Lance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanceRepository extends JpaRepository<Lance, Long> {
    List<Lance> findByAssembleiaIdOrderByValorOfertaDesc(Long assembleiaId);
}
