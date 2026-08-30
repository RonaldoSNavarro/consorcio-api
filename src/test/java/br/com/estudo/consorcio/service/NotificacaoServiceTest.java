package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.NotificacaoDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private PreferenciaNotificacaoRepository preferenciaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificacaoService service;

    private Cliente cliente;
    private Cota cota;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setEmail("maria@teste.com");

        cota = new Cota();
        cota.setId(10L);
        cota.setCodigoCota(100);
        cota.setCliente(cliente);
    }

    @Test
    @DisplayName("Deve enviar notificação regulatória mesmo sem preferência explícita")
    void deveEnviarNotificacaoRegulatoria() {
        Notificacao salva = Notificacao.builder()
                .id(1L)
                .tipo(TipoNotificacao.CONTEMPLACAO)
                .status(StatusNotificacao.ENVIADA)
                .titulo("Parabéns")
                .mensagem("Você foi contemplado")
                .cliente(cliente)
                .cota(cota)
                .build();

        when(notificacaoRepository.save(any(Notificacao.class))).thenReturn(salva);

        NotificacaoDTO resultado = service.criarENotificar(
                TipoNotificacao.CONTEMPLACAO, cliente, cota, "Parabéns", "Você foi contemplado", CanalNotificacao.EMAIL
        );

        assertNotNull(resultado);
        assertEquals(TipoNotificacao.CONTEMPLACAO, resultado.tipo());
        verify(emailService).enviarEmail(eq("maria@teste.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve respeitar opt-out de notificação de marketing")
    void deveRespeitarOptOutMarketing() {
        PreferenciaNotificacao pref = PreferenciaNotificacao.builder()
                .cliente(cliente)
                .categoria(TipoNotificacao.MARKETING.name())
                .habilitado(false)
                .build();

        when(preferenciaRepository.findByClienteIdAndCategoria(1L, "MARKETING")).thenReturn(Optional.of(pref));

        NotificacaoDTO resultado = service.criarENotificar(
                TipoNotificacao.MARKETING, cliente, cota, "Oferta", "Novo grupo", CanalNotificacao.EMAIL
        );

        assertNull(resultado);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve marcar notificação como lida com sucesso")
    void deveMarcarComoLida() {
        Notificacao notif = Notificacao.builder()
                .id(5L)
                .status(StatusNotificacao.ENVIADA)
                .cliente(cliente)
                .build();

        when(notificacaoRepository.findById(5L)).thenReturn(Optional.of(notif));
        when(notificacaoRepository.save(any(Notificacao.class))).thenAnswer(i -> i.getArgument(0));

        NotificacaoDTO resultado = service.marcarComoLida(5L);

        assertNotNull(resultado);
        assertEquals(StatusNotificacao.LIDA, resultado.status());
        assertNotNull(resultado.dataLeitura());
    }
}