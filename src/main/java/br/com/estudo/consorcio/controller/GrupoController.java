package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "Grupos", description = "Administração das regras financeiras, prazos, taxas e inauguração de novos grupos de consórcio.")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @Operation(summary = "Criar novo grupo", description = "Define os parâmetros do grupo: valor do crédito, taxa de administração e prazo total em meses. O status inicial é definido automaticamente como 'EM_FORMACAO'.")
    @PostMapping
    public ResponseEntity<GrupoResponseDTO> cadastrar(@Valid @RequestBody GrupoRequestDTO dto) {
        GrupoResponseDTO grupoSalvo = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoSalvo);
    }

    @Operation(summary = "Listar Grupos", description = "Lista todos os grupos cadastrados no sistema com seus respectivos dados financeiros e status.")
    @GetMapping
    public ResponseEntity<List<GrupoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @Operation(summary = "Inaugurar grupo", description = "Altera o status do grupo para 'EM_ANDAMENTO'. Esta operação valida se o grupo está em formação e registra a data da 1ª AGO.")
    @PutMapping("/{id}/inaugurar")
    public ResponseEntity<GrupoResponseDTO> inaugurar(@PathVariable Long id, @RequestParam LocalDate dataAssembleia) {
        GrupoResponseDTO grupoInaugurado = service.inaugurar(id, dataAssembleia);
        return ResponseEntity.ok(grupoInaugurado);
    }
}