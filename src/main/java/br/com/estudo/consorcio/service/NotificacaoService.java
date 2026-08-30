package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.NotificacaoDTO;
import br.com.estudo.consorcio.domain.dto.PreferenciaNotificacaoDTO;
import br.com.estudo.consorcio.domain.enums.CanalNotificacao;
import br.com.estudo.consorcio.domain.enums.StatusNotificacao;
import br.com.estudo.consorcio.domain.enums.TipoNotificacao;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Notificacao;
import br.com.estudo.consorcio.domain.model.PreferenciaNotificacao;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.NotificacaoRepository;
import br.com.estudo.consorcio.domain.repository.PreferenciaNotificacaoRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final PreferenciaNotificacaoRepository preferenciaRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              PreferenciaNotificacaoRepository preferenciaRepository,
                              ClienteRepository clienteRepository,
                              EmailService emailService) {
        this.notificacaoRepository = notificacaoRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.clienteRepository = clienteRepository;
        this.emailService = emailService;
    }

    @Transactional
    public NotificacaoDTO criarENotificar(TipoNotificacao tipo, Cliente cliente, Cota cota, String titulo, String mensagem, CanalNotificacao canal) {
        // Regulatória não permite opt-out
        boolean isRegulatoria = (tipo == TipoNotificacao.ASSEMBLEIA || tipo == TipoNotificacao.CONTEMPLACAO || tipo == TipoNotificacao.INADIMPLENCIA);

        if (!isRegulatoria && cliente != null) {
            Optional<PreferenciaNotificacao> pref = preferenciaRepository.findByClienteIdAndCategoria(cliente.getId(), tipo.name());
            if (pref.isPresent() && Boolean.FALSE.equals(pref.get().getHabilitado())) {
                return null; // Opt-out respeitado
            }
        }

        Notificacao notif = Notificacao.builder()
                .tipo(tipo)
                .canal(canal != null ? canal : CanalNotificacao.EMAIL)
                .status(StatusNotificacao.PENDENTE)
                .destinatarioEmail(cliente != null ? cliente.getEmail() : null)
                .cliente(cliente)
                .cota(cota)
                .titulo(titulo)
                .mensagem(mensagem)
                .retentativas(0)
                .createdAt(LocalDateTime.now())
                .build();

        notif = notificacaoRepository.save(notif);

        // Disparo assíncrono / simulado
        try {
            if (cliente != null && cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
                emailService.enviarEmail(cliente.getEmail(), titulo, mensagem);
            }
            notif.setStatus(StatusNotificacao.ENVIADA);
            notif.setDataEnvio(LocalDateTime.now());
        } catch (Exception e) {
            notif.setStatus(StatusNotificacao.FALHA);
            notif.setErroMensagem(e.getMessage());
        }

        notif = notificacaoRepository.save(notif);
        return toDTO(notif);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarNaoLidas(Long clienteId) {
        return notificacaoRepository.findByClienteIdAndStatusOrderByCreatedAtDesc(clienteId, StatusNotificacao.ENVIADA)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarTodasPorCliente(Long clienteId) {
        return notificacaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public NotificacaoDTO marcarComoLida(Long notificacaoId) {
        Notificacao notif = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada com ID: " + notificacaoId));
        notif.setStatus(StatusNotificacao.LIDA);
        notif.setDataLeitura(LocalDateTime.now());
        return toDTO(notificacaoRepository.save(notif));
    }

    @Transactional
    public PreferenciaNotificacaoDTO atualizarPreferencia(Long clienteId, String categoria, Boolean habilitado) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        PreferenciaNotificacao pref = preferenciaRepository.findByClienteIdAndCategoria(clienteId, categoria)
                .orElseGet(() -> PreferenciaNotificacao.builder()
                        .cliente(cliente)
                        .categoria(categoria)
                        .build());

        pref.setHabilitado(habilitado);
        pref.setUpdatedAt(LocalDateTime.now());
        pref = preferenciaRepository.save(pref);

        return new PreferenciaNotificacaoDTO(pref.getCategoria(), pref.getHabilitado());
    }

    private NotificacaoDTO toDTO(Notificacao n) {
        return new NotificacaoDTO(
                n.getId(),
                n.getTipo(),
                n.getCanal(),
                n.getStatus(),
                n.getTitulo(),
                n.getMensagem(),
                n.getDataEnvio(),
                n.getDataLeitura(),
                n.getCliente() != null ? n.getCliente().getId() : null,
                n.getCota() != null ? n.getCota().getId() : null
        );
    }
}