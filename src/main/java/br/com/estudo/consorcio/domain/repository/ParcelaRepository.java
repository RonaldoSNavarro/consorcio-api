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

    // Busca as parcelas de uma cota por status, ordenando da última para a primeira
    List<Parcela> findByCotaIdAndStatusOrderByNumeroParcelaDesc(Long cotaId, StatusParcela status);

    // Busca as parcelas pendentes na ordem natural (1, 2, 3...)
    List<Parcela> findByCotaIdAndStatusOrderByNumeroParcelaAsc(Long cotaId, StatusParcela status);

    // JPQL: Soma o Fundo Comum de um grupo inteiro, mas apenas das parcelas pagas!
    @Query("SELECT COALESCE(SUM(p.valorFundoComum), 0) FROM Parcela p WHERE p.cota.grupo.id = :grupoId AND p.status = :status")
    BigDecimal somarFundoComumPorGrupoEStatus(@Param("grupoId") Long grupoId, @Param("status") StatusParcela status);

    // Módulo 1: Busca parcelas pendentes/atrasadas de um grupo para reajuste
    List<Parcela> findByCotaGrupoIdAndStatusIn(Long grupoId, List<StatusParcela> statuses);

    // Módulo 2: Soma de taxas de administração e fundos de reserva por grupo e status
    @Query("SELECT COALESCE(SUM(p.valorTaxaAdministracao), 0) FROM Parcela p WHERE p.cota.grupo.id = :grupoId AND p.status = :status")
    BigDecimal somarTaxaAdministracaoPorGrupoEStatus(@Param("grupoId") Long grupoId, @Param("status") StatusParcela status);

    @Query("SELECT COALESCE(SUM(p.valorFundoReserva), 0) FROM Parcela p WHERE p.cota.grupo.id = :grupoId AND p.status = :status")
    BigDecimal somarFundoReservaPorGrupoEStatus(@Param("grupoId") Long grupoId, @Param("status") StatusParcela status);

    // Módulo 3: Verifica se a cota possui alguma parcela pendente/atrasada antes da data informada
    boolean existsByCotaIdAndStatusAndDataVencimentoBefore(Long cotaId, StatusParcela status, java.time.LocalDate date);

    // Módulo 2: Conta parcelas não pagas (pendentes ou atrasadas) de um grupo
    long countByCotaGrupoIdAndStatusIn(Long grupoId, List<StatusParcela> statuses);
}