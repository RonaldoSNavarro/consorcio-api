package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.RendimentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RendimentoFinanceiroRepository extends JpaRepository<RendimentoFinanceiro, Long> {
    List<RendimentoFinanceiro> findByGrupoId(Long grupoId);
}
