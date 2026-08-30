package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.SimulacaoApuracaoResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.CredenciamentoLanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssembleiaAvancadaServiceTest {

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private CredenciamentoLanceRepository credenciamentoRepository;

    @InjectMocks
    private AssembleiaAvancadaService service;

    private Assembleia assembleia;
    private Grupo grupo;
    private Cota cota;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(5L);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        assembleia = new Assembleia();
        assembleia.setId(10L);
        assembleia.setGrupo(grupo);

        cota = new Cota();
        cota.setId(100L);
        cota.setCodigoCota(8);
        cota.setStatus(StatusCota.ATIVA);
    }

    @Test
    @DisplayName("Deve simular apuração de assembleia com cálculo de saldo e contemplações previstas")
    void deveSimularApuracao() {
        when(assembleiaRepository.findById(10L)).thenReturn(Optional.of(assembleia));
        when(parcelaRepository.somarFundoComumPorGrupoEStatus(5L, StatusParcela.PAGA)).thenReturn(new BigDecimal("120000.00"));
        when(cotaRepository.findByGrupoId(5L)).thenReturn(List.of(cota));
        when(credenciamentoRepository.findByAssembleiaIdAndStatus(10L, StatusCredenciamento.ATIVO)).thenReturn(new ArrayList<>());

        SimulacaoApuracaoResponseDTO simulacao = service.simularApuracao(10L);

        System.out.println("DEBUG TEST SIMULACAO: " + simulacao);

        assertNotNull(simulacao);
        assertEquals(10L, simulacao.assembleiaId());
        assertEquals(1, simulacao.totalSorteadosPrevistos());
        assertNotNull(simulacao.hashPreviaSha256());
    }

    @Test
    @DisplayName("Deve gerar hash SHA-256 de auditoria para ata de assembleia")
    void deveGerarAuditTrailSha256() {
        assembleia.setNumeroSorteado(15);
        when(assembleiaRepository.findById(10L)).thenReturn(Optional.of(assembleia));

        String hash = service.gerarAuditTrailSha256(10L);

        assertNotNull(hash);
        assertTrue(hash.startsWith("SHA256-"));
    }
}