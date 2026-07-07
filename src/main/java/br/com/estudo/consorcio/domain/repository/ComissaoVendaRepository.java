package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ComissaoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComissaoVendaRepository extends JpaRepository<ComissaoVenda, Long> {
    java.util.Optional<ComissaoVenda> findByContratoIdAndStatus(Long contratoId, String status);
}
