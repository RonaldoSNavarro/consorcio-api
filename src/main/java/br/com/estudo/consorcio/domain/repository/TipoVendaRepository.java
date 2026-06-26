package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.TipoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoVendaRepository extends JpaRepository<TipoVenda, Long> {
    List<TipoVenda> findByAtivoTrue();
    boolean existsByNome(String nome);
}
