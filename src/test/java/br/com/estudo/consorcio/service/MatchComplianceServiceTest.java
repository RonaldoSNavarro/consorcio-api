package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchComplianceServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ListaRestritivaRepository listaRestritivaRepository;

    @Mock
    private AlertaComplianceRepository alertaComplianceRepository;

    @InjectMocks
    private MatchComplianceService matchComplianceService;

    @Captor
    private ArgumentCaptor<List<AlertaCompliance>> alertasCaptor;

    @BeforeEach
    void setUp() {
        matchComplianceService.setSimilarityThreshold(0.90);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Deve salvar alertas quando existirem matches do IBGE, PEP e OFAC/ONU")
    void testCruzarBaseDeClientes() {
        AlertaComplianceRepository.MatchResultProjection ibgeMatch = mock(AlertaComplianceRepository.MatchResultProjection.class);
        when(ibgeMatch.getClienteId()).thenReturn(1L);
        when(ibgeMatch.getListaId()).thenReturn(10L);
        when(ibgeMatch.getScore()).thenReturn(1.0);

        AlertaComplianceRepository.MatchResultProjection pepMatch = mock(AlertaComplianceRepository.MatchResultProjection.class);
        when(pepMatch.getClienteId()).thenReturn(2L);
        when(pepMatch.getListaId()).thenReturn(20L);
        when(pepMatch.getScore()).thenReturn(0.95);

        when(alertaComplianceRepository.findIbgeMatches()).thenReturn(List.of(ibgeMatch));
        when(alertaComplianceRepository.findPepMatches(0.90)).thenReturn(List.of(pepMatch));
        when(alertaComplianceRepository.findOfacOnuMatches(0.90)).thenReturn(List.of());

        Cliente cliente1 = new Cliente(); cliente1.setId(1L);
        Cliente cliente2 = new Cliente(); cliente2.setId(2L);
        ListaRestritiva lista1 = new ListaRestritiva(); lista1.setId(10L);
        ListaRestritiva lista2 = new ListaRestritiva(); lista2.setId(20L);

        when(clienteRepository.getReferenceById(1L)).thenReturn(cliente1);
        when(clienteRepository.getReferenceById(2L)).thenReturn(cliente2);
        when(listaRestritivaRepository.getReferenceById(10L)).thenReturn(lista1);
        when(listaRestritivaRepository.getReferenceById(20L)).thenReturn(lista2);

        matchComplianceService.cruzarBaseDeClientes();

        verify(alertaComplianceRepository, times(2)).saveAll(alertasCaptor.capture());
        
        List<List<AlertaCompliance>> allSaved = alertasCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertEquals(2, allSaved.size());
        
        List<AlertaCompliance> alertasIbge = allSaved.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(1, alertasIbge.size());
        org.junit.jupiter.api.Assertions.assertEquals(1L, alertasIbge.get(0).getCliente().getId());
        
        List<AlertaCompliance> alertasPep = allSaved.get(1);
        org.junit.jupiter.api.Assertions.assertEquals(1, alertasPep.size());
        org.junit.jupiter.api.Assertions.assertEquals(2L, alertasPep.get(0).getCliente().getId());
    }
}
