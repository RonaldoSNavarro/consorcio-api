package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.*;
import br.com.estudo.consorcio.service.CotaService;
import br.com.estudo.consorcio.service.ParcelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotas")
@Tag(name = "Cotas", description = "Gerenciamento das participações individuais, vinculação de clientes aos grupos, cancelamentos, reembolsos e inadimplências.")
public class CotaController {

    private final CotaService service;
    private final ParcelaService parcelaService;

    public CotaController(CotaService service, ParcelaService parcelaService) {
        this.service = service;
        this.parcelaService = parcelaService;
    }

    @Operation(summary = "Cadastrar cota", description = "Vincula um cliente a um grupo específico, gerando um número de cota único e definindo o status inicial como 'ATIVA'.")
    @PostMapping
    public ResponseEntity<CotaResponseDTO> cadastrar(@Valid @RequestBody CotaRequestDTO dto) {
        CotaResponseDTO cotaSalva = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cotaSalva);
    }

    @Operation(summary = "Listar cotas", description = "Exibe todas as cotas.")
    @GetMapping
    public ResponseEntity<List<CotaResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(summary = "Listar por cliente", description = "Lista cotas por cliente")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<CotaResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    @Operation(summary = "Listar por grupo", description = "Lista cotas por grupo.")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<CotaResponseDTO>> listarPorGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }

    @Operation(summary = "Cancelar cota", description = "Cancela uma cota ativa ou inadimplente, excluindo suas parcelas pendentes.")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CotaResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelarCota(id));
    }

    @Operation(summary = "Reembolsar cota cancelada", description = "Realiza o cálculo e o reembolso dos valores pagos ao Fundo Comum para cotas excluídas, aplicando multa penal de 10%.")
    @PostMapping("/{id}/reembolsar")
    public ResponseEntity<CotaReembolsoResponseDTO> reembolsar(@PathVariable Long id) {
        return ResponseEntity.ok(service.reembolsarCota(id));
    }

    @Operation(summary = "Obter inadimplência da cota", description = "Calcula detalhadamente as parcelas vencidas e juros/multas acumulados de uma cota.")
    @GetMapping("/{id}/inadimplencia")
    public ResponseEntity<CotaInadimplenciaResponseDTO> obterInadimplencia(@PathVariable Long id) {
        return ResponseEntity.ok(parcelaService.obterInadimplenciaCota(id));
    }

    @Operation(summary = "Histórico de transições de versões", description = "Retorna todos os logs de mudanças de estado e versionamento de uma cota específica.")
    @GetMapping("/{id}/versoes")
    public ResponseEntity<List<HistoricoVersaoCotaResponseDTO>> listarVersoes(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarVersoes(id));
    }
}