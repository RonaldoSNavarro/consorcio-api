package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.CategoriaBem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
import java.util.Optional;

@Repository
public interface CategoriaBemRepository extends JpaRepository<CategoriaBem, Long> {
    Optional<CategoriaBem> findByTipoBacen(TipoCategoriaBacen tipoBacen);
    Optional<CategoriaBem> findByNome(String nome);
}
