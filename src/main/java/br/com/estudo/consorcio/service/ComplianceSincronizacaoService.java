package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ComplianceSincronizacaoService {

    private final ListaRestritivaRepository listaRestritivaRepository;
    private final MatchComplianceService matchComplianceService;

    public ComplianceSincronizacaoService(ListaRestritivaRepository listaRestritivaRepository,
                                          MatchComplianceService matchComplianceService) {
        this.listaRestritivaRepository = listaRestritivaRepository;
        this.matchComplianceService = matchComplianceService;
    }

    @Async
    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // Rodar às 3:00 AM
    public void sincronizarListas() {
        // Simulação da chamada RestTemplate/Feign para o Governo/ONU/OFAC
        // Em um ambiente real, aqui seriam feitos requests paginados ou download de CSV/XML.

        listaRestritivaRepository.deleteAll(); // Reseta para simplificar (substituição total)

        inserirMock("OSAMA BIN LADEN", null, OrigemListaRestritiva.ONU);
        inserirMock("JOHN DOE TERRORIST", null, OrigemListaRestritiva.OFAC);
        inserirMock("POLITICO CORRUPTO SILVA", "111.222.333-44", OrigemListaRestritiva.PEP);
        inserirMock("RICARDO GARCIA SANCHES", "135.932.078-42", OrigemListaRestritiva.OFAC); // Do print do manual

        // Após popular a lista do dia, cruza a base de clientes
        matchComplianceService.cruzarBaseDeClientes();
    }

    private void inserirMock(String nome, String documento, OrigemListaRestritiva origem) {
        ListaRestritiva item = new ListaRestritiva();
        item.setNome(nome);
        item.setDocumentoOrigem(documento);
        item.setOrigem(origem);
        item.setDataInclusao(LocalDateTime.now());
        listaRestritivaRepository.save(item);
    }
}
