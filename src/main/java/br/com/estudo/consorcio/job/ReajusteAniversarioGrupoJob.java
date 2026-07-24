package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.service.GrupoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReajusteAniversarioGrupoJob {

    private static final Logger logger = LoggerFactory.getLogger(ReajusteAniversarioGrupoJob.class);

    private final GrupoRepository grupoRepository;
    private final GrupoService grupoService;

    public ReajusteAniversarioGrupoJob(GrupoRepository grupoRepository, GrupoService grupoService) {
        this.grupoRepository = grupoRepository;
        this.grupoService = grupoService;
    }

    // Executa no 1º dia de cada mês às 02:00
    @Scheduled(cron = "0 0 2 1 * *")
    public void executarReajustesAniversario() {
        int mesAtual = LocalDate.now().getMonthValue();
        logger.info("Iniciando rotina de reajuste automático de aniversário dos grupos para o mês {}...", mesAtual);

        List<Grupo> gruposEmAndamento = grupoRepository.findByStatusAndMesReajuste(StatusGrupo.EM_ANDAMENTO, mesAtual);

        for (Grupo grupo : gruposEmAndamento) {
            try {
                if (grupo.getIndiceReajuste() != null && grupo.getIndiceReajuste() != br.com.estudo.consorcio.domain.model.IndiceReajuste.MANUAL) {
                    logger.info("Executando reajuste anual por índice {} para o Grupo ID {} ({})", grupo.getIndiceReajuste(), grupo.getId(), grupo.getCodigo());
                    grupoService.reajustarGrupoPorIndice(grupo.getId(), grupo.getIndiceReajuste());
                }
            } catch (Exception e) {
                logger.error("Erro ao reajustar automaticamente o Grupo ID {}: {}", grupo.getId(), e.getMessage());
            }
        }
        logger.info("Rotina de reajuste automático de aniversário finalizada.");
    }
}
