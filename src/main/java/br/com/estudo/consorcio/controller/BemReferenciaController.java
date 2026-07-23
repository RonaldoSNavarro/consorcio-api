package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.*;
import br.com.estudo.consorcio.service.BemReferenciaService;
import br.com.estudo.consorcio.service.FipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bens-referencia")
@Tag(name = "Bens de Referência", description = "Gerenciamento de bens de referência, atualização de valores via FIPE/INCC e histórico de preços.")
public class BemReferenciaController {

    private final BemReferenciaService service;
    private final FipeService fipeService;

    public BemReferenciaController(BemReferenciaService service, FipeService fipeService) {
        this.service = service;
        this.fipeService = fipeService;
    }

    @Operation(summary = "Listar categorias de bem")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/categorias")
    public ResponseEntity<List<br.com.estudo.consorcio.domain.model.CategoriaBem>> listarCategorias() {
        return ResponseEntity.ok(service.listarCategorias());
    }

    @Operation(summary = "Listar bens de referência", description = "Lista de forma paginada os bens de referência cadastrados, com filtro opcional por categoria.")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping
    public ResponseEntity<Page<BemReferenciaResponseDTO>> listar(
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listar(categoriaId, pageable));
    }

    @Operation(summary = "Listar todos os bens ativos", description = "Lista todos os bens de referência ativos sem paginação para formulários.")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/todos")
    public ResponseEntity<List<BemReferenciaResponseDTO>> listarTodosAtivos() {
        return ResponseEntity.ok(service.listarTodosAtivos());
    }

    @Operation(summary = "Obter bem de referência por ID")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/{id}")
    public ResponseEntity<BemReferenciaResponseDTO> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }

    @Operation(summary = "Cadastrar bem de referência")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping
    public ResponseEntity<BemReferenciaResponseDTO> cadastrar(@Valid @RequestBody BemReferenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }

    @Operation(summary = "Atualizar bem de referência", description = "Atualiza a descrição, valor atual e registra a alteração no histórico de preços.")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PutMapping("/{id}")
    public ResponseEntity<BemReferenciaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody BemReferenciaRequestDTO dto,
            @RequestParam(required = false, defaultValue = "MANUAL") String origemReajuste) {
        return ResponseEntity.ok(service.atualizar(id, dto, origemReajuste));
    }

    @Operation(summary = "Consultar histórico de preços")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoValorResponseDTO>> obterHistorico(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterHistorico(id));
    }

    // --- ENDPOINTS INTEGRAÇÃO FIPE ---

    @Operation(summary = "FIPE: Listar Marcas", description = "Consulta a API FIPE (Parallelum) para retornar marcas de veículos.")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/fipe/marcas")
    public ResponseEntity<List<FipeItemDTO>> listarMarcasFipe() {
        return ResponseEntity.ok(fipeService.listarMarcas());
    }

    @Operation(summary = "FIPE: Listar Modelos")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/fipe/marcas/{marcaId}/modelos")
    public ResponseEntity<FipeModelosResponseDTO> listarModelosFipe(@PathVariable String marcaId) {
        return ResponseEntity.ok(fipeService.listarModelos(marcaId));
    }

    @Operation(summary = "FIPE: Listar Anos")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/fipe/marcas/{marcaId}/modelos/{modeloId}/anos")
    public ResponseEntity<List<FipeItemDTO>> listarAnosFipe(@PathVariable String marcaId, @PathVariable String modeloId) {
        return ResponseEntity.ok(fipeService.listarAnos(marcaId, modeloId));
    }

    @Operation(summary = "FIPE: Consultar Valor Oficial")
    @PreAuthorize("hasAnyAuthority('MANAGE_GRUPOS', 'VIEW_GRUPOS')")
    @GetMapping("/fipe/consultar")
    public ResponseEntity<FipeValorDTO> consultarValorFipe(
            @RequestParam String marcaId,
            @RequestParam String modeloId,
            @RequestParam String anoId) {
        FipeValorDTO result = fipeService.consultarValor(marcaId, modeloId, anoId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}
