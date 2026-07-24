package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AlertaComplianceResponseDTO;
import br.com.estudo.consorcio.domain.dto.ComplianceConfigDTO;
import br.com.estudo.consorcio.domain.dto.DeliberarAlertaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ComplianceSyncResultDTO;
import br.com.estudo.consorcio.domain.dto.ComplianceExecucaoLogResponseDTO;
import br.com.estudo.consorcio.domain.mapper.ComplianceExecucaoLogMapper;
import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.ComplianceConfig;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ComplianceConfigRepository;
import br.com.estudo.consorcio.repository.ComplianceExecucaoLogRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import br.com.estudo.consorcio.service.ComplianceSincronizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
@Tag(name = "Compliance", description = "Endpoints para monitoramento de PLD/FT e gestão de listas restritivas")
@SecurityRequirement(name = "bearer-key")
public class ComplianceController {

    private final ComplianceSincronizacaoService sincronizacaoService;
    private final AlertaComplianceRepository alertaRepository;
    private final ComplianceConfigRepository configRepository;
    private final ComplianceExecucaoLogRepository execucaoLogRepository;
    private final ComplianceExecucaoLogMapper execucaoLogMapper;

    public ComplianceController(ComplianceSincronizacaoService sincronizacaoService,
                                AlertaComplianceRepository alertaRepository,
                                ComplianceConfigRepository configRepository,
                                ComplianceExecucaoLogRepository execucaoLogRepository,
                                ComplianceExecucaoLogMapper execucaoLogMapper) {
        this.sincronizacaoService = sincronizacaoService;
        this.alertaRepository = alertaRepository;
        this.configRepository = configRepository;
        this.execucaoLogRepository = execucaoLogRepository;
        this.execucaoLogMapper = execucaoLogMapper;
    }

    @Operation(summary = "Sincronização manual de listas restritivas",
            description = "Dispara a rotina assíncrona de ingestão das bases PEP (Portal da Transparência), OFAC e ONU, executando em seguida o cruzamento (matching) com a base de clientes cadastrados.")
    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<ComplianceSyncResultDTO> sincronizarListasManualmente() {
        sincronizacaoService.sincronizarListas();
        return ResponseEntity.accepted().body(ComplianceSyncResultDTO.iniciado());
    }

    @Operation(summary = "Listar alertas de compliance",
            description = "Retorna todos os alertas gerados a partir do cruzamento de clientes contra as listas restritivas. Permite filtragem opcional por status do alerta.")
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<List<AlertaComplianceResponseDTO>> listarAlertas(
            @Parameter(description = "Status do alerta para filtragem (PENDENTE_ANALISE, FALSO_POSITIVO, CONFIRMADO)")
            @RequestParam(required = false) StatusAlertaCompliance status) {

        List<AlertaCompliance> alertas;
        if (status != null) {
            alertas = alertaRepository.findByStatus(status);
        } else {
            alertas = alertaRepository.findAll();
        }

        List<AlertaComplianceResponseDTO> response = alertas.stream().map(a -> new AlertaComplianceResponseDTO(
                a.getId(),
                a.getCliente().getId(),
                a.getCliente().getNome(),
                a.getCliente().getCpfCnpj(),
                a.getListaRestritiva().getOrigem(),
                a.getListaRestritiva().getNome(),
                a.getScore(),
                a.getStatus(),
                a.getDataDeteccao(),
                a.getJustificativa()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar logs de execucao de compliance",
            description = "Retorna o historico de execucao de sincronizacao de listas.")
    @GetMapping("/execucoes")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<List<ComplianceExecucaoLogResponseDTO>> listarExecucoes() {
        return ResponseEntity.ok(execucaoLogRepository.findTop50ByOrderByDataExecucaoDesc().stream()
                .map(execucaoLogMapper::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Deliberar sobre alerta de compliance",
            description = "Permite registrar o veredito do analista de compliance (FALSO_POSITIVO ou CONFIRMADO) para um determinado alerta, com o fornecimento obrigatório de uma justificativa formal.")
    @PutMapping("/alertas/{alertaId}/deliberar")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<Void> deliberarSobreAlerta(
            @Parameter(description = "ID do alerta de compliance") @PathVariable Long alertaId,
            @Valid @RequestBody DeliberarAlertaRequestDTO request) {

        AlertaCompliance alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));

        alerta.setStatus(request.novoStatus());
        alerta.setJustificativa(alerta.getJustificativa() + " | Deliberação: " + request.justificativa());
        alertaRepository.save(alerta);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Upload de arquivo PEP (CSV)",
            description = "Recebe e processa o arquivo CSV contendo os CPFs mascarados e nomes de Pessoas Expostas Politicamente.")
    @PostMapping(value = "/upload/pep", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<Map<String, Object>> uploadPep(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RegraDeNegocioException("O arquivo de upload não pode estar vazio.");
        }
        try {
            int count = sincronizacaoService.processarPepCsv(file.getInputStream());
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Arquivo PEP processado com sucesso. " + count + " registros inseridos/atualizados."
            ));
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro ao processar arquivo PEP: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload de arquivo ONU (XML)",
            description = "Recebe e processa o arquivo XML contendo indivíduos e entidades sancionados pelo Conselho de Segurança da ONU.")
    @PostMapping(value = "/upload/onu", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<Map<String, Object>> uploadOnu(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RegraDeNegocioException("O arquivo de upload não pode estar vazio.");
        }
        try {
            int count = sincronizacaoService.processarOnuXml(file.getInputStream());
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Arquivo ONU processado com sucesso. " + count + " registros inseridos/atualizados."
            ));
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro ao processar arquivo ONU: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload de arquivo IBGE (XLS)",
            description = "Recebe e processa a planilha XLS contendo os Municípios de Faixa de Fronteira e Cidades Gêmeas do IBGE.")
    @PostMapping(value = "/upload/ibge", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<Map<String, Object>> uploadIbge(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RegraDeNegocioException("O arquivo de upload não pode estar vazio.");
        }
        try {
            int count = sincronizacaoService.processarIbgeXls(file.getInputStream());
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Arquivo IBGE processado com sucesso. " + count + " municípios indexados."
            ));
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro ao processar arquivo IBGE: " + e.getMessage());
        }
    }

    @Operation(summary = "Obter configuração do agendador cron",
            description = "Retorna a frequência, o horário de disparo e a expressão cron atual do job automático de compliance.")
    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<ComplianceConfigDTO> getConfig() {
        ComplianceConfig config = getOrCreateConfig();

        return ResponseEntity.ok(new ComplianceConfigDTO(
                config.getCronExpression(),
                config.getFrequencia(),
                config.getHorario(),
                config.getDataAtualizacao()
        ));
    }

    @Operation(summary = "Atualizar configuração do agendador cron",
            description = "Permite alterar a frequência (DIARIO, SEMANAL, MENSAL) e o horário (HH:mm) do processamento automático. O trigger do agendamento é recalculado em tempo de execução.")
    @PutMapping("/config")
    @PreAuthorize("hasAnyAuthority('VIEW_COMPLIANCE')")
    public ResponseEntity<ComplianceConfigDTO> updateConfig(
            @Valid @RequestBody ComplianceConfigDTO request) {
        
        ComplianceConfig config = getOrCreateConfig();

        String cron = gerarCronExpression(request.frequencia(), request.horario());

        config.setFrequencia(request.frequencia().toUpperCase());
        config.setHorario(request.horario());
        config.setCronExpression(cron);
        config.setDataAtualizacao(LocalDateTime.now());
        
        configRepository.save(config);

        return ResponseEntity.ok(new ComplianceConfigDTO(
                config.getCronExpression(),
                config.getFrequencia(),
                config.getHorario(),
                config.getDataAtualizacao()
        ));
    }

    private ComplianceConfig getOrCreateConfig() {
        return configRepository.findById(1L).orElseGet(() -> {
            ComplianceConfig newConfig = new ComplianceConfig();
            newConfig.setId(1L);
            newConfig.setCronExpression("0 0 3 * * *");
            newConfig.setFrequencia("DIARIO");
            newConfig.setHorario("03:00");
            newConfig.setDataAtualizacao(LocalDateTime.now());
            return configRepository.save(newConfig);
        });
    }

    private String gerarCronExpression(String frequencia, String horario) {
        if (horario == null || !horario.contains(":")) {
            throw new RegraDeNegocioException("Horário inválido. Formato esperado: HH:mm");
        }
        String[] timeParts = horario.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        switch (frequencia.toUpperCase()) {
            case "DIARIO":
                return String.format("0 %d %d * * *", minute, hour);
            case "SEMANAL":
                return String.format("0 %d %d * * MON", minute, hour);
            case "MENSAL":
                return String.format("0 %d %d 1 * *", minute, hour);
            default:
                throw new RegraDeNegocioException("Frequência de agendamento inválida. Valores aceitos: DIARIO, SEMANAL, MENSAL");
        }
    }
}
