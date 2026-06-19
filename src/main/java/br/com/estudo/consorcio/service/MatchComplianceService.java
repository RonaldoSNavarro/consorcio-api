package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchComplianceService {

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

    @Transactional
    public void cruzarBaseDeClientes() {
        List<Cliente> clientes = clienteRepository.findAll();
        List<ListaRestritiva> listas = listaRestritivaRepository.findAll();

        JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();

        for (Cliente cliente : clientes) {
            for (ListaRestritiva lista : listas) {
                // Checa CPF exato
                boolean cpfMatch = lista.getDocumentoOrigem() != null &&
                        lista.getDocumentoOrigem().equals(cliente.getCpfCnpj());

                // Checa similaridade de nome
                double scoreNome = jaroWinkler.apply(cliente.getNome().toUpperCase(), lista.getNome().toUpperCase());

                if (cpfMatch || scoreNome >= 0.90) {
                    registrarAlertaSeNaoExistir(cliente, lista, java.math.BigDecimal.valueOf(cpfMatch ? 1.0 : scoreNome));
                }
            }
        }
    }

    private void registrarAlertaSeNaoExistir(Cliente cliente, ListaRestritiva lista, java.math.BigDecimal score) {
        // Na prática, checaríamos se já existe um alerta pendente para esse par (cliente_id, lista_id)
        // Para simplificar, vamos inserir.
        AlertaCompliance alerta = new AlertaCompliance();
        alerta.setCliente(cliente);
        alerta.setListaRestritiva(lista);
        alerta.setScore(score);
        alerta.setStatus(StatusAlertaCompliance.PENDENTE_ANALISE);
        alerta.setDataDeteccao(LocalDateTime.now());
        alerta.setJustificativa("Alerta gerado automaticamente pelo cruzamento noturno. Score: " + score);

        alertaComplianceRepository.save(alerta);
    }
}
