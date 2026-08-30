package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.LoteriaFederal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LoteriaFederalRepository extends JpaRepository<LoteriaFederal, Long> {
    Optional<LoteriaFederal> findTopByDataSorteioLessThanEqualOrderByDataSorteioDesc(LocalDate dataSorteio);
    Optional<LoteriaFederal> findByConcurso(String concurso);
}
