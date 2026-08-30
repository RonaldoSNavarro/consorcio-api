package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.service.ContabilidadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Provisiona o plano COSIF simplificado antes de a aplicação receber operações financeiras.
 * O catálogo corresponde às contas exigidas pelos fluxos definidos na ADR 002.
 */
@Component
public class PlanoContasCosifInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanoContasCosifInitializer.class);

    private final ContabilidadeService contabilidadeService;

    /**
     * Cria o inicializador do plano de contas.
     *
     * @param contabilidadeService serviço responsável pelo catálogo e ledger COSIF
     */
    public PlanoContasCosifInitializer(ContabilidadeService contabilidadeService) {
        this.contabilidadeService = contabilidadeService;
    }

    /**
     * Provisiona idempotentemente as contas padronizadas após a criação do contexto Spring.
     *
     * @param args argumentos de inicialização da aplicação
     */
    @Override
    public void run(ApplicationArguments args) {
        int contasCriadas = contabilidadeService.provisionarPlanoContasOperacional();
        LOGGER.info("Plano COSIF validado: {} conta(s) provisionada(s) nesta inicialização.", contasCriadas);
    }
}
