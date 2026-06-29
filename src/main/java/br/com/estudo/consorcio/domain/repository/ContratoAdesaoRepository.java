package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContratoAdesaoRepository extends JpaRepository<ContratoAdesao, Long> {
    Optional<ContratoAdesao> findByNumeroContrato(String numeroContrato);
}
