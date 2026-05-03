package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Parcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {

    // Método do Spring para buscar todas as parcelas de uma cota específica
    List<Parcela> findByCotaId(Long cotaId);
}