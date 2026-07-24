package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.IndiceEconomico;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndiceEconomicoRepository extends JpaRepository<IndiceEconomico, Long> {

    List<IndiceEconomico> findTop12ByTipoIndiceOrderByDataReferenciaDesc(IndiceReajuste tipoIndice);

    Optional<IndiceEconomico> findByTipoIndiceAndDataReferencia(IndiceReajuste tipoIndice, LocalDate dataReferencia);

    List<IndiceEconomico> findByTipoIndiceAndDataReferenciaBetweenOrderByDataReferenciaAsc(
            IndiceReajuste tipoIndice, LocalDate inicio, LocalDate fim);
}
