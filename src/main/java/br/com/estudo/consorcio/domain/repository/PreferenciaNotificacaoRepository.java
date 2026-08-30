package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.PreferenciaNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacao, Long> {
    List<PreferenciaNotificacao> findByClienteId(Long clienteId);
    Optional<PreferenciaNotificacao> findByClienteIdAndCategoria(Long clienteId, String categoria);
}