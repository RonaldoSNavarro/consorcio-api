package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.enums.StatusNotificacao;
import br.com.estudo.consorcio.domain.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByClienteIdAndStatusOrderByCreatedAtDesc(Long clienteId, StatusNotificacao status);
    List<Notificacao> findByClienteIdOrderByCreatedAtDesc(Long clienteId);
    List<Notificacao> findByStatusOrderByCreatedAtAsc(StatusNotificacao status);
}