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

    @BeforeEach
    void setUp() {
        matchComplianceService.setSimilarityThreshold(0.90);
    }

    @ParameterizedTest
    @CsvSource({
            "Ronaldo Navarro, Ronaldo Navarro, true",
            "Ronaldo Navarro, Ronaaldo Navarro, true",
            "Roberto da Silva, Roberrto da Silva, true",
            "João Paulo, Joao Paulo, true",
            "Marcos Antonio, Marcos Antonio Junior, true",
            "Ronaldo Navarro, Carlos Eduardo, false",
            "Roberto da Silva, Joao da Silva, false",
            "Marcos Antonio, Marcos, false"
    })
    @DisplayName("Teste parametrizado - Algoritmo Jaro-Winkler para similaridade de nomes na ONU")
    void testJaroWinklerSimilarity(String nomeCliente, String nomeLista, boolean expectedMatch) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome(nomeCliente);
        cliente.setCpfCnpj("11122233344");
        
        // Use a PageImpl to support the upcoming pagination refactor (R4)
        Page<Cliente> clientePage = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAll(any(Pageable.class))).thenReturn(clientePage).thenReturn(Page.empty());

        ListaRestritiva lista = new ListaRestritiva();
        lista.setId(10L);
        lista.setNome(nomeLista);
        lista.setOrigem(OrigemListaRestritiva.ONU);

        when(listaRestritivaRepository.findAll()).thenReturn(List.of(lista));

        if (expectedMatch) {
            when(alertaComplianceRepository.existsByClienteIdAndListaRestritivaId(1L, 10L)).thenReturn(false);
        }

        matchComplianceService.cruzarBaseDeClientes();

        if (expectedMatch) {
            verify(alertaComplianceRepository, times(1)).save(any(AlertaCompliance.class));
        } else {
            verify(alertaComplianceRepository, never()).save(any(AlertaCompliance.class));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "Marcelo Antonio Carreira, 111.531.324-88, MARCELO ANTONIO CARREIRA, ***.531.324-**, true",
            "Marcelo A Carreira, 111.531.324-88, MARCELO ANTONIO CARREIRA, ***.531.324-**, true",
            "Pedro Albuquerque, 111.531.324-88, MARCELO ANTONIO CARREIRA, ***.531.324-**, false",
            "Marcelo Antonio Carreira, 111.999.324-88, MARCELO ANTONIO CARREIRA, ***.531.324-**, false"
    })
    @DisplayName("Teste parametrizado - Regra PEP combinando CPF mascarado e Jaro-Winkler")
    void testPepCpfMaskingAndJaroWinkler(String nomeCliente, String cpfCliente, String nomeLista, String documentoLista, boolean expectedMatch) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome(nomeCliente);
        cliente.setCpfCnpj(cpfCliente);
        
        Page<Cliente> clientePage = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAll(any(Pageable.class))).thenReturn(clientePage).thenReturn(Page.empty());

        ListaRestritiva lista = new ListaRestritiva();
        lista.setId(20L);
        lista.setNome(nomeLista);
        lista.setDocumentoOrigem(documentoLista);
        lista.setOrigem(OrigemListaRestritiva.PEP);

        when(listaRestritivaRepository.findAll()).thenReturn(List.of(lista));

        if (expectedMatch) {
            when(alertaComplianceRepository.existsByClienteIdAndListaRestritivaId(1L, 20L)).thenReturn(false);
        }

        matchComplianceService.cruzarBaseDeClientes();

        if (expectedMatch) {
            verify(alertaComplianceRepository, times(1)).save(any(AlertaCompliance.class));
        } else {
            verify(alertaComplianceRepository, never()).save(any(AlertaCompliance.class));
        }
    }
}
