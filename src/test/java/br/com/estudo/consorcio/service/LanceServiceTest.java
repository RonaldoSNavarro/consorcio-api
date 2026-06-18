package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.LanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LanceServiceTest {

    @Mock
    private LanceRepository lanceRepository;

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Spy
    private br.com.estudo.consorcio.domain.mapper.LanceMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.LanceMapper.class);

    @InjectMocks
    private LanceService service;

    private Grupo grupo;
    private Assembleia assembleia;
    private Cota cota;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(1L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        grupo.setPercentualLanceEmbutidoMaximo(new BigDecimal("0.3000"));
        grupo.setPercentualLanceFixo(new BigDecimal("0.2000"));

        assembleia = new Assembleia();
        assembleia.setId(2L);
        assembleia.setGrupo(grupo);
        assembleia.setStatus(StatusAssembleia.CAPTANDO);

        cota = new Cota();
        cota.setId(3L);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);
    }

    @Test
    @DisplayName("Deve registrar um Lance Livre válido com sucesso")
    void deveRegistrarLanceLivreValido() {
        // Arrange
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, new BigDecimal("15000.00"), ModalidadeLance.LIVRE);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);
        when(lanceRepository.save(any(Lance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LanceResponseDTO response = service.registrarLance(request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("15000.00"), response.valorOferta());
        assertEquals(ModalidadeLance.LIVRE, response.modalidade());
        verify(lanceRepository, times(1)).save(any(Lance.class));
    }

    @Test
    @DisplayName("Deve barrar Lance Livre com valor nulo")
    void deveBarrarLanceLivreComValorNulo() {
        // Arrange
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, null, ModalidadeLance.LIVRE);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // Act & Assert
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> service.registrarLance(request));
        assertTrue(exception.getMessage().contains("Valor da oferta deve ser informado"));
        verify(lanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve barrar Lance Livre com valor menor ou igual a zero")
    void deveBarrarLanceLivreComValorZerado() {
        // Arrange
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, BigDecimal.ZERO, ModalidadeLance.LIVRE);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // Act & Assert
        assertThrows(RegraDeNegocioException.class, () -> service.registrarLance(request));
        verify(lanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar Lance Fixo calculando o valor automaticamente a partir da parametrização do grupo")
    void deveRegistrarLanceFixoComCalculoAutomatico() {
        // Arrange - Note que valorOferta no request é nulo, e a modalidade é FIXO
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, null, ModalidadeLance.FIXO);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);
        when(lanceRepository.save(any(Lance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LanceResponseDTO response = service.registrarLance(request);

        // Assert
        assertNotNull(response);
        // 100.000,00 * 20% = 20.000,00
        assertEquals(new BigDecimal("20000.00"), response.valorOferta());
        assertEquals(ModalidadeLance.FIXO, response.modalidade());
        verify(lanceRepository, times(1)).save(any(Lance.class));
    }

    @Test
    @DisplayName("Deve registrar Lance Fixo assumindo o valor padrão de 20% se o percentual do grupo for nulo")
    void deveRegistrarLanceFixoComDefaultSeGrupoNulo() {
        // Arrange
        grupo.setPercentualLanceFixo(null); // Sem configuração explícita
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, null, ModalidadeLance.FIXO);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);
        when(lanceRepository.save(any(Lance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LanceResponseDTO response = service.registrarLance(request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("20000.00"), response.valorOferta());
        verify(lanceRepository, times(1)).save(any(Lance.class));
    }

    @Test
    @DisplayName("Deve barrar e lançar exceção ao cadastrar lance se a cota possuir parcelas atrasadas")
    void deveBarrarLancePorInadimplencia() {
        // Arrange
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, new BigDecimal("15000.00"), ModalidadeLance.LIVRE);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        // Cota possui parcelas em atraso
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(true);

        // Act & Assert
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> service.registrarLance(request));
        assertTrue(exception.getMessage().contains("parcelas em atraso"));
        verify(lanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve barrar Lance Embutido acima do limite configurado")
    void deveBarrarLanceEmbutidoAcimaDoLimite() {
        // Arrange - Lance embutido de 35% (limite é 30%)
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.EMBUTIDO, new BigDecimal("35000.00"), ModalidadeLance.LIVRE);

        when(assembleiaRepository.findById(2L)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(3L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(3L), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // Act & Assert
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> service.registrarLance(request));
        assertTrue(exception.getMessage().contains("ultrapassa o teto do grupo"));
        verify(lanceRepository, never()).save(any());
    }
}
