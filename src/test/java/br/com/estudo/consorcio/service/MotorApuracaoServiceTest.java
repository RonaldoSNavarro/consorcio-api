package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorApuracaoServiceTest {

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private LanceRepository lanceRepository;

    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private ContemplacaoService contemplacaoService;

    @InjectMocks
    private MotorApuracaoService motorApuracaoService;

    private Grupo grupo;
    private Assembleia assembleia;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(1L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        grupo.setCriterioDesempateLance(CriterioDesempateLance.LOTERIA_FEDERAL);

        assembleia = new Assembleia();
        assembleia.setId(2L);
        assembleia.setGrupo(grupo);
        assembleia.setStatus(StatusAssembleia.REALIZADA);
    }

    @Test
    @DisplayName("REQ-CON-006 AC1: Deve classificar Lance Fixo com base na Loteria Federal (cota mais próxima da pedra sorteada)")
    void deveApurarVencedorLanceFixoDesempate() {
        // Arrange
        // Cota 12, Cota 25, Cota 45
        // Pedra sorteada = 30
        assembleia.setNumeroSorteado(30);

        Cota cota12 = new Cota(); cota12.setId(12L); cota12.setNumeroCota(12); cota12.setGrupo(grupo);
        Cota cota25 = new Cota(); cota25.setId(25L); cota25.setNumeroCota(25); cota25.setGrupo(grupo);
        Cota cota45 = new Cota(); cota45.setId(45L); cota45.setNumeroCota(45); cota45.setGrupo(grupo);

        Lance lance12 = new Lance(1L, cota12, assembleia, TipoLance.FIRME, new BigDecimal("20000.00"), LocalDateTime.now(), StatusApuracaoLance.CADASTRADO, ModalidadeLance.FIXO, 0L);
        Lance lance25 = new Lance(2L, cota25, assembleia, TipoLance.FIRME, new BigDecimal("20000.00"), LocalDateTime.now(), StatusApuracaoLance.CADASTRADO, ModalidadeLance.FIXO, 0L);
        Lance lance45 = new Lance(3L, cota45, assembleia, TipoLance.FIRME, new BigDecimal("20000.00"), LocalDateTime.now(), StatusApuracaoLance.CADASTRADO, ModalidadeLance.FIXO, 0L);

        List<Lance> lances = List.of(lance12, lance25, lance45);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(2L)).thenReturn(lances);
        
        // Saldo Fundo Comum comporta apenas uma contemplação (Crédito 100k - Lance 20k = 80k necessários)
        when(contabilidadeService.calcularSaldoConta(eq(grupo), eq(ContabilidadeService.CONTA_FUNDO_COMUM)))
                .thenReturn(new BigDecimal("80000.00"));

        // Act
        motorApuracaoService.apurarAssembleia(2L);

        // Assert
        // A cota 25 é a mais próxima de 30 (distancia 5, contra 15 da cota 45 e 18 da cota 12)
        assertEquals(StatusApuracaoLance.VENCEDOR, lance25.getStatusApuracao());
        assertEquals(StatusApuracaoLance.PERDEDOR, lance12.getStatusApuracao());
        assertEquals(StatusApuracaoLance.PERDEDOR, lance45.getStatusApuracao());

        ArgumentCaptor<ContemplacaoRequestDTO> captor = ArgumentCaptor.forClass(ContemplacaoRequestDTO.class);
        verify(contemplacaoService, times(1)).registrar(captor.capture());
        
        ContemplacaoRequestDTO registeredContemplation = captor.getValue();
        assertEquals(25L, registeredContemplation.cotaId());
        assertEquals(TipoContemplacao.LANCE_FIXO, registeredContemplation.tipoContemplacao());
        assertEquals(new BigDecimal("20000.00"), registeredContemplation.valorLance());

        verify(lanceRepository, times(3)).save(any(Lance.class));
        assertEquals(StatusAssembleia.FECHADA, assembleia.getStatus());
    }

    @Test
    @DisplayName("Deve barrar e marcar como perdedor caso o saldo do Fundo Comum seja insuficiente")
    void deveMarcarComoPerdedorSeSaldoInsuficiente() {
        // Arrange
        assembleia.setNumeroSorteado(30);

        Cota cota25 = new Cota(); cota25.setId(25L); cota25.setNumeroCota(25); cota25.setGrupo(grupo);
        Lance lance25 = new Lance(2L, cota25, assembleia, TipoLance.FIRME, new BigDecimal("20000.00"), LocalDateTime.now(), StatusApuracaoLance.CADASTRADO, ModalidadeLance.FIXO, 0L);

        List<Lance> lances = List.of(lance25);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(2L)).thenReturn(lances);
        
        // Saldo Fundo Comum insuficiente (R$ 50k, necessário R$ 80k)
        when(contabilidadeService.calcularSaldoConta(eq(grupo), eq(ContabilidadeService.CONTA_FUNDO_COMUM)))
                .thenReturn(new BigDecimal("50000.00"));

        // Act
        motorApuracaoService.apurarAssembleia(2L);

        // Assert
        assertEquals(StatusApuracaoLance.PERDEDOR, lance25.getStatusApuracao());
        verify(contemplacaoService, never()).registrar(any());
        verify(lanceRepository, times(1)).save(lance25);
    }
}
