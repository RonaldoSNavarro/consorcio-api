package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoConsorcioRepository extends JpaRepository<ProdutoConsorcio, Long> {
    List<ProdutoConsorcio> findByAtivoTrue();
}
