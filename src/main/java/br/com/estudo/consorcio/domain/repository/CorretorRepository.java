package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Corretor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorretorRepository extends JpaRepository<Corretor, Long> {
}
