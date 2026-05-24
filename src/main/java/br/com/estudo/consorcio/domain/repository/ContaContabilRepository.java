package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ContaContabil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaContabilRepository extends JpaRepository<ContaContabil, Long> {
    Optional<ContaContabil> findByCodigoCosif(String codigoCosif);
}
