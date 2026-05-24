package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.GrupoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "Grupos", description = "Administração das regras financeiras, prazos, taxas, reajustes e encerramento de grupos de consórcio.")
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

    @Operation(summary = "Listar Grupos", description = "Lista grupos cadastrados no sistema de forma paginada.")
    @GetMapping
    public ResponseEntity<Page<GrupoResponseDTO>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodos(pageable));
    }

    @Operation(summary = "Inaugurar grupo", description = "Altera o status do grupo para 'EM_ANDAMENTO'. Esta operação valida se o grupo está em formação e registra a data da 1ª AGO.")
    @PutMapping("/{id}/inaugurar")
    public ResponseEntity<GrupoResponseDTO> inaugurar(@PathVariable Long id, @RequestParam LocalDate dataAssembleia) {
        GrupoResponseDTO grupoInaugurado = service.inaugurar(id, dataAssembleia);
        return ResponseEntity.ok(grupoInaugurado);
    }

    @Operation(summary = "Reajustar valor do crédito", description = "Atualiza o valor do crédito do grupo e reajusta todas as parcelas em aberto proporcionalmente.")
    @PutMapping("/{id}/reajuste")
    public ResponseEntity<GrupoResponseDTO> reajustar(@PathVariable Long id, @RequestParam BigDecimal novoValorCredito) {
        GrupoResponseDTO grupoReajustado = service.reajustarGrupo(id, novoValorCredito);
        return ResponseEntity.ok(grupoReajustado);
    }

    @Operation(summary = "Relatório financeiro do grupo", description = "Consolida a arrecadação de Fundo Comum, Fundo de Reserva, Taxa de Administração e créditos já pagos.")
    @GetMapping("/{id}/financeiro")
    public ResponseEntity<GrupoFinanceiroResponseDTO> obterFinanceiro(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterRelatorioFinanceiro(id));
    }

    @Operation(summary = "Encerrar grupo", description = "Encerra as atividades do grupo de consórcio caso todas as obrigações financeiras de todas as cotas tenham sido quitadas.")
    @PostMapping("/{id}/encerrar")
    public ResponseEntity<GrupoResponseDTO> encerrar(@PathVariable Long id) {
        GrupoResponseDTO grupoEncerrado = service.encerrarGrupo(id);
        return ResponseEntity.ok(grupoEncerrado);
    }
}