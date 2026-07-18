package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.List;

@Service
public class MatchComplianceService {

    @Value("${compliance.similarity.threshold:0.90}")
    private double similarityThreshold = 0.90;

    private final ClienteRepository clienteRepository;
    private final ListaRestritivaRepository listaRestritivaRepository;
    private final AlertaComplianceRepository alertaComplianceRepository;

    public MatchComplianceService(ClienteRepository clienteRepository,
                                  ListaRestritivaRepository listaRestritivaRepository,
                                  AlertaComplianceRepository alertaComplianceRepository) {
        this.clienteRepository = clienteRepository;
        this.listaRestritivaRepository = listaRestritivaRepository;
        this.alertaComplianceRepository = alertaComplianceRepository;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public void cruzarBaseDeClientes() {
        // 1. IBGE matches
        List<AlertaComplianceRepository.MatchResultProjection> ibgeMatches = alertaComplianceRepository.findIbgeMatches();
        salvarAlertas(ibgeMatches, "Cliente domiciliado em município de faixa de fronteira ou cidade gêmea.");

        // 2. PEP matches
        List<AlertaComplianceRepository.MatchResultProjection> pepMatches = alertaComplianceRepository.findPepMatches(this.similarityThreshold);
        salvarAlertas(pepMatches, "Match de CPF e Nome na lista de PEP (Pessoas Expostas Politicamente).");

        // 3. OFAC/ONU matches
        List<AlertaComplianceRepository.MatchResultProjection> ofacOnuMatches = alertaComplianceRepository.findOfacOnuMatches(this.similarityThreshold);
        salvarAlertas(ofacOnuMatches, "Similaridade de nome identificada na lista de sanções internacionais.");
    }

    private void salvarAlertas(List<AlertaComplianceRepository.MatchResultProjection> matches, String justificativaBase) {
        if (matches == null || matches.isEmpty()) return;

        List<AlertaCompliance> novosAlertas = new java.util.ArrayList<>();
        for (AlertaComplianceRepository.MatchResultProjection match : matches) {
            Cliente clienteProxy = clienteRepository.getReferenceById(match.getClienteId());
            ListaRestritiva listaProxy = listaRestritivaRepository.getReferenceById(match.getListaId());

            AlertaCompliance alerta = new AlertaCompliance();
            alerta.setCliente(clienteProxy);
            alerta.setListaRestritiva(listaProxy);
            alerta.setScore(java.math.BigDecimal.valueOf(match.getScore()));
            alerta.setStatus(StatusAlertaCompliance.PENDENTE_ANALISE);
            alerta.setDataDeteccao(LocalDateTime.now());
            alerta.setJustificativa(justificativaBase + " | Score: " + match.getScore());
            novosAlertas.add(alerta);
        }
        alertaComplianceRepository.saveAll(novosAlertas);
    }
}
