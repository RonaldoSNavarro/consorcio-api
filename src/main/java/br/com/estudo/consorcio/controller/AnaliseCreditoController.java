package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AnaliseCreditoRequestDTO;
import br.com.estudo.consorcio.domain.dto.AnaliseCreditoResponseDTO;
import br.com.estudo.consorcio.service.AnaliseCreditoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analises-credito")
@Tag(name = "Análise de Crédito", description = "Módulo de Avaliação de Crédito (Pós-Contemplação)")
public class AnaliseCreditoController {

    private final AnaliseCreditoService analiseCreditoService;

    public AnaliseCreditoController(AnaliseCreditoService analiseCreditoService) {
        this.analiseCreditoService = analiseCreditoService;
    }

    @PostMapping("/avaliar")
    @Operation(summary = "Avaliar Análise de Crédito de Cota Contemplada", description = "Valida a margem consignável (30% da renda) e as garantias. Aprova ou reprova a liberação de crédito.")
    public ResponseEntity<AnaliseCreditoResponseDTO> avaliarAnalise(@RequestBody AnaliseCreditoRequestDTO dto) {
        AnaliseCreditoResponseDTO response = analiseCreditoService.avaliarAnalise(dto);
        return ResponseEntity.ok(response);
    }
}
