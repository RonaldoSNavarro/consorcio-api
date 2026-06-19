package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListaRestritivaRepository extends JpaRepository<ListaRestritiva, Long> {
    List<ListaRestritiva> findByDocumentoOrigem(String documentoOrigem);
}
