package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.service.ClienteService;
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

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Gerenciamento de consorciados (pessoas físicas e jurídicas) e seus dados cadastrais.")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
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
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {

        // Sem cast desnecessário — o service já retorna Page<ClienteResponseDTO>
        return ResponseEntity.ok(service.listarTodos(pageable));
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
}