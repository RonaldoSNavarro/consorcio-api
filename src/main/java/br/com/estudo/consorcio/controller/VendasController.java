package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ContratoResponseDTO;
import br.com.estudo.consorcio.domain.dto.PropostaRequestDTO;
import br.com.estudo.consorcio.domain.dto.PropostaResponseDTO;
import br.com.estudo.consorcio.domain.mapper.PropostaAdesaoMapper;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.service.PropostaAdesaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendasController {

    private final PropostaAdesaoService propostaService;
    private final PropostaAdesaoMapper mapper;

    @PostMapping("/propostas")
    public ResponseEntity<PropostaResponseDTO> criarProposta(@RequestBody @Valid PropostaRequestDTO request) {
        PropostaAdesao proposta = propostaService.criarProposta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(proposta));
    }

    @PostMapping("/propostas/{id}/aprovar")
    public ResponseEntity<ContratoResponseDTO> aprovarProposta(@PathVariable Long id) {
        ContratoAdesao contrato = propostaService.aprovarProposta(id);
        return ResponseEntity.ok(mapper.toContratoResponse(contrato));
    }

    @PostMapping("/contratos/{id}/efetivar")
    public ResponseEntity<ContratoResponseDTO> efetivarContrato(@PathVariable Long id) {
        ContratoAdesao contrato = propostaService.efetivarContrato(id);
        return ResponseEntity.ok(mapper.toContratoResponse(contrato));
    }
}
