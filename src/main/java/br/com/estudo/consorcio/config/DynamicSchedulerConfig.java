package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.model.ComplianceConfig;
import br.com.estudo.consorcio.domain.repository.ComplianceConfigRepository;
import br.com.estudo.consorcio.service.ComplianceSincronizacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Optional;

@Slf4j
@Configuration
@EnableScheduling
public class DynamicSchedulerConfig implements SchedulingConfigurer {

    private final ComplianceConfigRepository configRepository;
    private final ComplianceSincronizacaoService sincronizacaoService;

    public DynamicSchedulerConfig(ComplianceConfigRepository configRepository,
                                  ComplianceSincronizacaoService sincronizacaoService) {
        this.configRepository = configRepository;
        this.sincronizacaoService = sincronizacaoService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("Iniciando execução agendada dinâmica de Compliance...");
                    try {
                        sincronizacaoService.sincronizarListas();
                        log.info("Execução agendada dinâmica de Compliance finalizada com sucesso.");
                    } catch (Exception e) {
                        log.error("Erro na execução agendada dinâmica de Compliance", e);
                    }
                },
                triggerContext -> {
                    Optional<ComplianceConfig> configOpt = configRepository.findById(1L);
                    String cron = configOpt.map(ComplianceConfig::getCronExpression).orElse("0 0 3 * * *");
                    
                    log.debug("Agendador dinâmico de Compliance calculando próxima execução com cron: '{}'", cron);
                    
                    CronTrigger trigger = new CronTrigger(cron);
                    return trigger.nextExecution(triggerContext);
                }
        );
    }
}
