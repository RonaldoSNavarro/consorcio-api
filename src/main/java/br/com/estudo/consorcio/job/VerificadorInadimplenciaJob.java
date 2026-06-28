package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.service.CotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class VerificadorInadimplenciaJob {

    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final CotaService cotaService;

    public VerificadorInadimplenciaJob(CotaRepository cotaRepository, ParcelaRepository parcelaRepository, CotaService cotaService) {
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.cotaService = cotaService;
    }

    /**
     * Executa todo dia à 1h da manhã.
     * Verifica cotas ativas e suspensas para aplicar as regras de inadimplência da Resolução BCB 285.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void verificarInadimplencia() {
        log.info("Iniciando rotina de verificação de inadimplência de cotas...");
        List<Cota> cotasAtivasESuspensas = cotaRepository.findAll().stream()
                .filter(c -> c.getStatus() == StatusCota.ATIVA || c.getStatus() == StatusCota.SUSPENSA)
                .toList();

        LocalDate hoje = LocalDate.now();

        for (Cota cota : cotasAtivasESuspensas) {
            List<Parcela> parcelasAtrasadas = parcelaRepository.findByCotaId(cota.getId()).stream()
                    .filter(p -> p.getStatus() == StatusParcela.PENDENTE && p.getDataVencimento().isBefore(hoje))
                    .toList();

            int totalAtrasadas = parcelasAtrasadas.size();

            if (totalAtrasadas >= 3) {
                // >= 3 parcelas em atraso: Excluir cota
                if (cota.getStatus() != StatusCota.EXCLUIDA) {
                    cotaService.registrarTransicaoVersao(cota, StatusCota.EXCLUIDA, "Exclusão por inadimplência (>= 3 parcelas).");
                    log.info("Cota {} excluída por inadimplência.", cota.getNumeroCota());
                }
            } else if (totalAtrasadas >= 1) {
                // 1 ou 2 parcelas em atraso: Suspender cota
                if (cota.getStatus() != StatusCota.SUSPENSA) {
                    cotaService.registrarTransicaoVersao(cota, StatusCota.SUSPENSA, "Suspensão por inadimplência (1 ou 2 parcelas).");
                    log.info("Cota {} suspensa por inadimplência.", cota.getNumeroCota());
                }
            } else {
                // 0 parcelas em atraso: Reativar cota se estiver suspensa
                if (cota.getStatus() == StatusCota.SUSPENSA) {
                    cotaService.registrarTransicaoVersao(cota, StatusCota.ATIVA, "Regularização de inadimplência.");
                    log.info("Cota {} reativada após regularização.", cota.getNumeroCota());
                }
            }
        }
        log.info("Rotina de verificação de inadimplência concluída.");
    }
}
