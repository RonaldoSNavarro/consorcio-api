package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropostaAdesaoRepository extends JpaRepository<PropostaAdesao, Long> {
    Optional<PropostaAdesao> findByNumeroProposta(String numeroProposta);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cliente", "produto"})
    List<PropostaAdesao> findByStatus(StatusProposta status);
}
