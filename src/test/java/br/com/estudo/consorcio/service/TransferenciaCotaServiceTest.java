package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.TransferenciaCotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TransferenciaCotaResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.TransferenciaCotaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaCotaServiceTest {

    @Mock
    private TransferenciaCotaRepository transferenciaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private TransferenciaCotaService service;

    private Cliente cedente;
    private Cliente cessionario;
    private Grupo grupo;
    private Cota cota;

    @BeforeEach
    void setUp() {
        cedente = new Cliente();
        cedente.setId(1L);
        cedente.setNome("Carlos Cedente");
        cedente.setCpfCnpj("11122233344");
        cedente.setStatus(StatusCliente.ATIVO);

        cessionario = new Cliente();
        cessionario.setId(2L);
        cessionario.setNome("Ana Cessionária");
        cessionario.setCpfCnpj("99988877766");
        cessionario.setStatus(StatusCliente.ATIVO);

        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setCodigoGrupo("GRP-10");
        grupo.setValorCredito(new BigDecimal("150000.00"));

        cota = new Cota();
        cota.setId(100L);
        cota.setCodigoCota(25);
        cota.setStatus(StatusCota.ATIVA);
        cota.setCliente(cedente);
        cota.setGrupo(grupo);
    }

    @Test
    @DisplayName("Deve solicitar transferência com sucesso")
    void deveSolicitarTransferencia() {
        TransferenciaCotaRequestDTO req = new TransferenciaCotaRequestDTO(100L, 2L, new BigDecimal("3000.00"), "Cessão amigável");

        when(cotaRepository.findById(100L)).thenReturn(Optional.of(cota));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cessionario));
        when(transferenciaRepository.save(any(TransferenciaCota.class))).thenAnswer(i -> {
            TransferenciaCota t = i.getArgument(0);
            t.setId(1L);
            return t;
        });

        TransferenciaCotaResponseDTO resp = service.solicitarTransferencia(req);

        assertNotNull(resp);
        assertEquals(StatusTransferencia.EM_ANALISE_CREDITO, resp.status());
        assertEquals("Carlos Cedente", resp.nomeCedente());
        assertEquals("Ana Cessionária", resp.nomeCessionario());
        verify(notificacaoService).criarENotificar(any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve rejeitar transferência de cota cancelada")
    void deveRejeitarTransferenciaCotaCancelada() {
        cota.setStatus(StatusCota.CANCELADA);
        TransferenciaCotaRequestDTO req = new TransferenciaCotaRequestDTO(100L, 2L, null, null);

        when(cotaRepository.findById(100L)).thenReturn(Optional.of(cota));

        assertThrows(RegraDeNegocioException.class, () -> service.solicitarTransferencia(req));
        verifyNoInteractions(transferenciaRepository);
    }

    @Test
    @DisplayName("Deve efetivar transferência e atualizar titular da cota")
    void deveEfetivarTransferencia() {
        TransferenciaCota transf = TransferenciaCota.builder()
                .id(1L)
                .cota(cota)
                .cedente(cedente)
                .cessionario(cessionario)
                .status(StatusTransferencia.PENDENTE_PAGAMENTO_TAXA)
                .dataSolicitacao(LocalDateTime.now())
                .taxaTransferencia(new BigDecimal("3000.00"))
                .build();

        when(transferenciaRepository.findById(1L)).thenReturn(Optional.of(transf));
        when(transferenciaRepository.save(any(TransferenciaCota.class))).thenAnswer(i -> i.getArgument(0));

        TransferenciaCotaResponseDTO resp = service.efetivarTransferencia(1L);

        assertNotNull(resp);
        assertEquals(StatusTransferencia.EFETIVADA, resp.status());
        assertEquals(cessionario, cota.getCliente());
        verify(cotaRepository).save(cota);
        verify(notificacaoService).criarENotificar(any(), any(), any(), anyString(), anyString(), any());
    }
}