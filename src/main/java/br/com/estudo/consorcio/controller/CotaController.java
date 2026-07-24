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
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

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
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping
    public ResponseEntity<CotaResponseDTO> cadastrar(@Valid @RequestBody CotaRequestDTO dto) {
        CotaResponseDTO cotaSalva = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cotaSalva);
    }

    @Operation(summary = "Listar cotas", description = "Exibe cotas com paginação.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping
    public ResponseEntity<Page<CotaResponseDTO>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodas(pageable));
    }

    @Operation(summary = "Buscar cotas com filtros", description = "Pesquisa cotas por grupo, código da cota, versão de histórico e/ou CPF/CNPJ do cliente.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<CotaResponseDTO>> buscar(
            @RequestParam(required = false) Long grupoId,
            @RequestParam(required = false) Integer codigoCota,
            @RequestParam(required = false) Integer versaoHistorico,
            @RequestParam(required = false) String cpfCnpj,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.buscar(grupoId, codigoCota, versaoHistorico, cpfCnpj, pageable));
    }

    @Operation(summary = "Listar por cliente", description = "Lista cotas por cliente")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Page<CotaResponseDTO>> listarPorCliente(@PathVariable Long clienteId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId, pageable));
    }

    @Operation(summary = "Listar por grupo", description = "Lista cotas por grupo.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<Page<CotaResponseDTO>> listarPorGrupo(@PathVariable Long grupoId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId, pageable));
    }

    @Operation(summary = "Buscar cota por ID", description = "Retorna os dados de uma cota específica pelo seu identificador.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/{id}")
    public ResponseEntity<CotaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Cancelar cota", description = "Cancela uma cota ativa ou inadimplente, excluindo suas parcelas pendentes.")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CotaResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelarCota(id));
    }

    @Operation(summary = "Listar Cotas Canceladas Pendentes de Reembolso", description = "Retorna a listagem de cotas canceladas elegíveis a reembolso com as simulações de valores.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/canceladas/pendentes-reembolso")
    public ResponseEntity<List<CotaReembolsoSimulacaoDTO>> listarPendentesReembolso() {
        return ResponseEntity.ok(service.listarPendentesReembolso());
    }

    @Operation(summary = "Reembolsar cota cancelada", description = "Realiza o cálculo e o reembolso dos valores pagos ao Fundo Comum para cotas excluídas, aplicando multa penal de 10%.")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping("/{id}/reembolsar")
    public ResponseEntity<CotaReembolsoResponseDTO> reembolsar(@PathVariable Long id) {
        return ResponseEntity.ok(service.reembolsarCota(id));
    }

    @Operation(summary = "Obter inadimplência da cota", description = "Calcula detalhadamente as parcelas vencidas e juros/multas acumulados de uma cota.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/{id}/inadimplencia")
    public ResponseEntity<CotaInadimplenciaResponseDTO> obterInadimplencia(@PathVariable Long id) {
        return ResponseEntity.ok(parcelaService.obterInadimplenciaCota(id));
    }

    @Operation(summary = "Histórico de transições de versões", description = "Retorna todos os logs de mudanças de estado e versionamento de uma cota específica.")
    @PreAuthorize("hasAuthority('VIEW_COTAS')")
    @GetMapping("/{id}/versoes")
    public ResponseEntity<List<HistoricoVersaoCotaResponseDTO>> listarVersoes(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarVersoes(id));
    }

    @Operation(summary = "Transferir cota para outro titular", description = "Transfere a titularidade de uma cota para outro cliente ativo e respeitando o limite de 10% do grupo.")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping("/{id}/transferir")
    public ResponseEntity<CotaResponseDTO> transferirCota(@PathVariable Long id, @Valid @RequestBody TransferirCotaRequestDTO dto) {
        return ResponseEntity.ok(service.transferirCota(id, dto));
    }

    @Operation(summary = "Readmitir consorciado excluído", description = "Readmite uma cota excluída caso regularize as pendências (Art. 31-A).")
    @PreAuthorize("hasAuthority('MANAGE_COTAS')")
    @PostMapping("/{id}/readmitir")
    public ResponseEntity<CotaResponseDTO> readmitirCota(@PathVariable Long id) {
        return ResponseEntity.ok(service.readmitirCota(id));
    }
}