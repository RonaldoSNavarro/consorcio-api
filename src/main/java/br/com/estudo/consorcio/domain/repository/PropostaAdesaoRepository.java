package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropostaAdesaoRepository extends JpaRepository<PropostaAdesao, Long> {
    Optional<PropostaAdesao> findByNumeroProposta(String numeroProposta);
}
