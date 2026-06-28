package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.LoteriaFederal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteriaFederalRepository extends JpaRepository<LoteriaFederal, Long> {
}
