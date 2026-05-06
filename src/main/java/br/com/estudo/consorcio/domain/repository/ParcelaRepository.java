package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {

    // Lista parcelas pelo id da cota
    List<Parcela> findByCotaId(Long cotaId);

    // JPQL: Soma o Fundo Comum de um grupo inteiro, mas apenas das parcelas pagas!
    @Query("SELECT COALESCE(SUM(p.valorFundoComum), 0) FROM Parcela p WHERE p.cota.grupo.id = :grupoId AND p.status = :status")
    BigDecimal somarFundoComumPorGrupoEStatus(@Param("grupoId") Long grupoId, @Param("status") StatusParcela status);
}