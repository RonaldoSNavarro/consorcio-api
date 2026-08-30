package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.CredenciamentoLanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredenciamentoLanceServiceTest {

    @Mock
    private CredenciamentoLanceRepository credenciamentoRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @InjectMocks
    private CredenciamentoLanceService service;

    private Cota cota;
    private Assembleia assembleia;

    @BeforeEach
    void setUp() {
        cota = new Cota();
        cota.setId(10L);
        cota.setCodigoCota(5);
        cota.setStatus(StatusCota.ATIVA);

        assembleia = new Assembleia();
        assembleia.setId(20L);
        assembleia.setStatus(StatusAssembleia.AGENDADA);
    }

    @Test
    @DisplayName("Deve credenciar lance prévio com sucesso gerando hash de auditoria")
    void deveCredenciarLanceComSucesso() {
        CredenciamentoLanceRequestDTO req = new CredenciamentoLanceRequestDTO(
                10L, 20L, TipoLance.FIRME, ModalidadeLance.LIVRE,
                new BigDecimal("15000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15000.00"), true
        );

        when(cotaRepository.findById(10L)).thenReturn(Optional.of(cota));
        when(assembleiaRepository.findById(20L)).thenReturn(Optional.of(assembleia));
        when(parcelaRepository.findByCotaId(10L)).thenReturn(List.of());
        when(credenciamentoRepository.findByCotaIdAndAssembleiaIdAndStatus(10L, 20L, StatusCredenciamento.ATIVO)).thenReturn(Optional.empty());
        when(credenciamentoRepository.save(any(CredenciamentoLance.class))).thenAnswer(i -> {
            CredenciamentoLance c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        CredenciamentoLanceResponseDTO resp = service.credenciarLance(req, "192.168.1.100");

        assertNotNull(resp);
        assertEquals(StatusCredenciamento.ATIVO, resp.status());
        assertNotNull(resp.hashAssinatura());
        assertEquals(new BigDecimal("15000.00"), resp.valorLance());
    }

    @Test
    @DisplayName("Deve bloquear credenciamento se houver parcela atrasada")
    void deveBloquearLanceComParcelaAtrasada() {
        Parcela pAtrasada = new Parcela();
        pAtrasada.setStatus(StatusParcela.ATRASADA);

        when(cotaRepository.findById(10L)).thenReturn(Optional.of(cota));
        when(assembleiaRepository.findById(20L)).thenReturn(Optional.of(assembleia));
        when(parcelaRepository.findByCotaId(10L)).thenReturn(List.of(pAtrasada));

        CredenciamentoLanceRequestDTO req = new CredenciamentoLanceRequestDTO(
                10L, 20L, TipoLance.FIRME, ModalidadeLance.LIVRE,
                new BigDecimal("15000.00"), null, null, null, true
        );

        assertThrows(RegraDeNegocioException.class, () -> service.credenciarLance(req, "127.0.0.1"));
    }
}