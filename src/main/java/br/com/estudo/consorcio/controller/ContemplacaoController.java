package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.service.ContemplacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contemplacoes")
@Tag(name = "Contemplações", description = "Endpoints para registro de sorteios, lances e liberação de crédito")
public class ContemplacaoController {

    private final ContemplacaoService service;

    public ContemplacaoController(ContemplacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Registra uma nova contemplação",
            description = "Processa sorteios e lances. Valida regras do Banco Central, incluindo trava de saldo do Fundo Comum e limite máximo de 30% para lances embutidos.")
    @PostMapping
    public ResponseEntity<ContemplacaoResponseDTO> registrar(@RequestBody ContemplacaoRequestDTO dto) {
        ContemplacaoResponseDTO salva = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @Operation(summary = "Lista contemplados por Assembleia",
            description = "Retorna o histórico de todas as cotas contempladas em uma assembleia específica.")
    @GetMapping("/assembleia/{assembleiaId}")
    public ResponseEntity<List<ContemplacaoResponseDTO>> listarPorAssembleia(@PathVariable Long assembleiaId) {
        return ResponseEntity.ok(service.listarPorAssembleia(assembleiaId));
    }
}