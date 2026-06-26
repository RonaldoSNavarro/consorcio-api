package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.*;
import br.com.estudo.consorcio.service.VendaPropostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas de Proposta", description = "Gestão de tipos de venda e efetivação de propostas de adesão com criação automática de cota.")
@SecurityRequirement(name = "bearer-key")
public class VendaPropostaController {

    private final VendaPropostaService service;

    public VendaPropostaController(VendaPropostaService service) {
        this.service = service;
    }

    // --- Tipos de Venda ---

    @Operation(summary = "Listar tipos de venda ativos")
    @GetMapping("/tipos")
    public ResponseEntity<List<TipoVendaResponseDTO>> listarTipos() {
        return ResponseEntity.ok(service.listarTiposVenda());
    }

    @Operation(summary = "Listar todos os tipos de venda (incluindo inativos)", description = "Requer perfil ADMIN.")
    @GetMapping("/tipos/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TipoVendaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodosTiposVenda());
    }

    @Operation(summary = "Criar novo tipo de venda")
    @PostMapping("/tipos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<TipoVendaResponseDTO> criar(@Valid @RequestBody TipoVendaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarTipoVenda(dto));
    }

    @Operation(summary = "Atualizar tipo de venda")
    @PutMapping("/tipos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<TipoVendaResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody TipoVendaRequestDTO dto) {
        return ResponseEntity.ok(service.atualizarTipoVenda(id, dto));
    }

    @Operation(summary = "Inativar tipo de venda")
    @DeleteMapping("/tipos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativarTipoVenda(id);
        return ResponseEntity.noContent().build();
    }

    // --- Efetivação de Venda ---

    @Operation(summary = "Efetivar venda de proposta de adesão",
            description = "Registra a adesão do cliente ao grupo selecionado, criando automaticamente a cota e gerando o plano de parcelas.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<CotaResponseDTO> efetivarVenda(@Valid @RequestBody VendaPropostaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.efetivarVenda(dto));
    }
}
