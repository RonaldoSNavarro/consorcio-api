package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.BoletoPixResponseDTO;
import br.com.estudo.consorcio.domain.dto.WebhookPixRequestDTO;
import br.com.estudo.consorcio.service.IntegracaoBancariaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/pagamentos")
@Tag(name = "Webhooks Bancários", description = "Endpoints de callback e conciliação bancária para liquidação de PIX e boletos")
public class WebhookPagamentoController {

    private final IntegracaoBancariaService service;

    public WebhookPagamentoController(IntegracaoBancariaService service) {
        this.service = service;
    }

    @Operation(summary = "Processar confirmação de pagamento PIX (Webhook do Banco)")
    @PostMapping("/pix")
    public ResponseEntity<BoletoPixResponseDTO> processarPix(@Valid @RequestBody WebhookPixRequestDTO dto) {
        return ResponseEntity.ok(service.processarWebhookPix(dto));
    }
}