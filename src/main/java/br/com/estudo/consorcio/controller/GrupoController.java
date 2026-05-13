package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Criar novo grupo", description = "Define os parâmetros do grupo: valor do crédito, taxa de administração, fundo de reserva e prazo total em meses.")
    @PostMapping
    public ResponseEntity<Grupo> cadastrar(@RequestBody Grupo grupo) {
        Grupo grupoSalvo = service.salvar(grupo);
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoSalvo);
    }

    @Operation(summary = "Listar Grupos", description = "Lista todos os grupos.")
    @GetMapping
    public ResponseEntity<List<Grupo>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Endpoint para inaugurar o grupo (ocorrerá no dia da 1ª AGO)
    @Operation(summary = "Inaugurar grupo", description = "Altera o status do grupo para 'EM_ANDAMENTO'. Esta operação valida se o quórum mínimo de cotas vendidas foi atingido conforme normas do BCB.")
    @PutMapping("/{id}/inaugurar")
    public ResponseEntity<Grupo> inaugurar(@PathVariable Long id, @RequestParam LocalDate dataAssembleia) {
        Grupo grupoInaugurado = service.inaugurar(id, dataAssembleia);
        return ResponseEntity.ok(grupoInaugurado);
    }
}