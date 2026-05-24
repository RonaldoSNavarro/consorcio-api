package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class LgpdAnonymizationJob {

    private final GrupoRepository grupoRepository;
    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;

    public LgpdAnonymizationJob(GrupoRepository grupoRepository, CotaRepository cotaRepository, ClienteRepository clienteRepository) {
        this.grupoRepository = grupoRepository;
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
    }

    /**
     * Executa todos os dias às 02:00 da manhã.
     * Varre grupos ENCERRADOS há mais de 10 anos (Retenção Legal do BCB).
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void executarExpurgoLgpd() {
        LocalDate limiteRetencao = LocalDate.now().minusYears(10);
        log.info("Iniciando rotina de expurgo LGPD. Data limite de encerramento: {}", limiteRetencao);

        List<Grupo> grupos = grupoRepository.findByStatusAndDataEncerramentoBefore(StatusGrupo.ENCERRADO, limiteRetencao);

        for (Grupo grupo : grupos) {
            log.info("Expurgando dados sensíveis do Grupo ID: {} (Encerrado em: {})", grupo.getId(), grupo.getDataEncerramento());

            List<Cota> cotas = cotaRepository.findByGrupoId(grupo.getId());
            for (Cota cota : cotas) {
                Cliente cliente = cota.getCliente();

                if (!cliente.getNome().startsWith("ANONIMIZADO")) {
                    cliente.setNome("ANONIMIZADO-" + cliente.getId());
                    // Gera um CPF/CNPJ fictício preenchendo com zeros e terminando com o ID
                    String maskDoc = String.format("%011d", cliente.getId());
                    cliente.setCpfCnpj(maskDoc);
                    cliente.setEmail("anonimizado" + cliente.getId() + "@consorcio.local");
                    cliente.setTelefone("00000000000");
                    cliente.setCep("00000000");
                    cliente.setLogradouro("DADOS EXPURGADOS (LGPD)");
                    cliente.setNumero("0");
                    cliente.setBairro("EXPURGADO");
                    cliente.setLocalidade("EXPURGADO");
                    cliente.setUf("EX");

                    clienteRepository.save(cliente);
                    log.debug("Cliente ID {} anonimizado com sucesso.", cliente.getId());
                }
            }

            // Atualiza status do grupo para não processar novamente e confirmar arquivamento profundo
            grupo.setStatus(StatusGrupo.EXPURGADO);
            grupoRepository.save(grupo);
            log.info("Grupo ID {} movido para status EXPURGADO.", grupo.getId());
        }
    }
}
