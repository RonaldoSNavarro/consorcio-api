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

    // @Transactional removido para evitar estouro do cache L1 do JPA em processamentos em lote
    public void cruzarBaseDeClientes() {
        List<ListaRestritiva> listas = listaRestritivaRepository.findAll();
        JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

        int pageSize = 1000;
        int pageNumber = 0;
        Page<Cliente> paginaClientes;

        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id"));
            paginaClientes = clienteRepository.findAll(pageable);

            for (Cliente cliente : paginaClientes.getContent()) {
            String clientCpfCentrais = obterDigitosCentraisCpf(cliente.getCpfCnpj());
            String localidade = cliente.getLocalidade() != null ? cliente.getLocalidade().trim() : "";
            String uf = cliente.getUf() != null ? cliente.getUf().trim() : "";
            String clientCityState = (localidade.isEmpty() && uf.isEmpty()) ? "" : normalizar(localidade + " - " + uf);

            for (ListaRestritiva lista : listas) {
                if (lista.getOrigem() == OrigemListaRestritiva.IBGE) {
                    // Match de Faixa de Fronteira / Cidade Gêmea
                    String nomeLista = lista.getNome() != null ? lista.getNome().trim() : "";
                    String borderCityState = normalizar(nomeLista);
                    if (!clientCityState.isEmpty() && clientCityState.equals(borderCityState)) {
                        registrarAlertaSeNaoExistir(cliente, lista, java.math.BigDecimal.valueOf(1.0),
                                "Cliente domiciliado em município de faixa de fronteira ou cidade gêmea.");
                    }
                } else if (lista.getOrigem() == OrigemListaRestritiva.PEP) {
                    // Match de PEP com CPF mascarado (ex: ***.531.324-**)
                    String pepCpfCentrais = obterDigitosCentraisCpfPep(lista.getDocumentoOrigem());
                    boolean cpfMatch = pepCpfCentrais != null && pepCpfCentrais.equals(clientCpfCentrais);
                    double scoreNome = jaroWinkler.apply(normalizar(cliente.getNome()), normalizar(lista.getNome()));

                    if (cpfMatch && scoreNome >= this.similarityThreshold) {
                        registrarAlertaSeNaoExistir(cliente, lista, java.math.BigDecimal.valueOf(scoreNome),
                                "Match de CPF e Nome na lista de PEP (Pessoas Expostas Politicamente). CPF centrais: " + clientCpfCentrais);
                    }
                } else {
                    // OFAC / ONU: Match por similaridade de nome
                    double scoreNome = jaroWinkler.apply(normalizar(cliente.getNome()), normalizar(lista.getNome()));
                    if (scoreNome >= this.similarityThreshold) {
                        registrarAlertaSeNaoExistir(cliente, lista, java.math.BigDecimal.valueOf(scoreNome),
                                "Similaridade de nome identificada na lista de sanções " + lista.getOrigem() + ".");
                    }
                }
            }
        }
        pageNumber++;
    } while (paginaClientes.hasNext());
}

    private void registrarAlertaSeNaoExistir(Cliente cliente, ListaRestritiva lista, java.math.BigDecimal score, String justificativa) {
        if (!alertaComplianceRepository.existsByClienteIdAndListaRestritivaId(cliente.getId(), lista.getId())) {
            AlertaCompliance alerta = new AlertaCompliance();
            alerta.setCliente(cliente);
            alerta.setListaRestritiva(lista);
            alerta.setScore(score);
            alerta.setStatus(StatusAlertaCompliance.PENDENTE_ANALISE);
            alerta.setDataDeteccao(LocalDateTime.now());
            alerta.setJustificativa(justificativa + " | Score: " + score);
            alertaComplianceRepository.save(alerta);
        }
    }

    private String obterDigitosCentraisCpf(String cpf) {
        if (cpf == null) return null;
        String apenasDigitos = cpf.replaceAll("\\D", "");
        if (apenasDigitos.length() == 11) {
            return apenasDigitos.substring(3, 9);
        }
        return null;
    }

    private String obterDigitosCentraisCpfPep(String pepCpf) {
        if (pepCpf == null) return null;
        String apenasDigitos = pepCpf.replaceAll("\\D", "");
        if (apenasDigitos.length() == 6) {
            return apenasDigitos;
        }
        if (apenasDigitos.length() == 11) {
            return apenasDigitos.substring(3, 9);
        }
        return null;
    }

    private String normalizar(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toUpperCase();
    }
}
