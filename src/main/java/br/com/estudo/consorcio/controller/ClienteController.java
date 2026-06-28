package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.dto.HistoricoConsorciadoResponseDTO;
import br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO;
import br.com.estudo.consorcio.domain.model.TipoInteracao;
import br.com.estudo.consorcio.service.ClienteService;
import br.com.estudo.consorcio.service.HistoricoConsorciadoService;
import br.com.estudo.consorcio.service.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Gerenciamento de consorciados (pessoas físicas e jurídicas) e seus dados cadastrais.")
public class ClienteController {

    private final ClienteService service;
    private final ViaCepService viaCepService;
    private final HistoricoConsorciadoService historicoService;

    public ClienteController(ClienteService service, ViaCepService viaCepService, HistoricoConsorciadoService historicoService) {
        this.service = service;
        this.viaCepService = viaCepService;
        this.historicoService = historicoService;
    }

    @Operation(summary = "Registrar novo cliente",
            description = "Cria um novo consorciado. Valida duplicidade de CPF/CNPJ.")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(
            @Valid @RequestBody ClienteRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.salvar(dto));
    }

    @Operation(summary = "Listar clientes com paginação",
            description = "Retorna os consorciados paginados. Padrão: 20 por página, ordenado por nome.")
    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {

        return ResponseEntity.ok(service.listarTodos(search, pageable));
    }

    @Operation(summary = "Buscar cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @Parameter(description = "ID do cliente") @PathVariable Long id) {

        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Atualizar dados do cliente",
            description = "Atualiza os dados cadastrais. CPF/CNPJ não podem ser alterados após o cadastro.")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO dto) {

        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Inativar cliente",
            description = "Inativação lógica — o registro é mantido por obrigação legal (LGPD Art. 16, inciso II).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar endereço por CEP",
            description = "Consulta a API externa do ViaCEP para obter os dados completos do endereço.")
    @GetMapping("/busca-cep/{cep}")
    public ResponseEntity<ViaCepResponseDTO> buscarCep(
            @Parameter(description = "CEP com 8 dígitos") @PathVariable String cep) {
        return ResponseEntity.ok(viaCepService.buscarCep(cep));
    }

    @Operation(summary = "Obter histórico completo do consorciado",
            description = "Retorna todas as interações e snapshots financeiros do cliente, filtrados opcionalmente por tipo de interação.")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoConsorciadoResponseDTO>> obterHistorico(
            @Parameter(description = "ID do cliente") @PathVariable Long id,
            @Parameter(description = "Tipo de interação para filtro") @RequestParam(required = false) TipoInteracao tipo) {

        // Valida acesso via IDOR internamente no ClienteService
        service.buscarPorId(id);

        if (tipo != null) {
            return ResponseEntity.ok(historicoService.listarPorClienteETipo(id, tipo));
        }
        return ResponseEntity.ok(historicoService.listarPorCliente(id));
    }
}