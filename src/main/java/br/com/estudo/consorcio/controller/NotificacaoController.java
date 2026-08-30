package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.NotificacaoDTO;
import br.com.estudo.consorcio.domain.dto.PreferenciaNotificacaoDTO;
import br.com.estudo.consorcio.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@Tag(name = "Notificações", description = "Central de notificações, inbox do consorciado e preferências de comunicação")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar notificações não lidas por cliente")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_COTAS')")
    @GetMapping("/cliente/{clienteId}/nao-lidas")
    public ResponseEntity<List<NotificacaoDTO>> listarNaoLidas(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarNaoLidas(clienteId));
    }

    @Operation(summary = "Listar histórico completo de notificações por cliente")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_COTAS')")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<NotificacaoDTO>> listarTodasPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarTodasPorCliente(clienteId));
    }

    @Operation(summary = "Marcar notificação como lida")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_COTAS')")
    @PutMapping("/{id}/marcar-lida")
    public ResponseEntity<NotificacaoDTO> marcarComoLida(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarComoLida(id));
    }

    @Operation(summary = "Atualizar preferências de notificação (LGPD opt-in/opt-out)")
    @PreAuthorize("hasAnyAuthority('VIEW_DASHBOARD', 'VIEW_COTAS')")
    @PostMapping("/cliente/{clienteId}/preferencias")
    public ResponseEntity<PreferenciaNotificacaoDTO> atualizarPreferencia(
            @PathVariable Long clienteId,
            @RequestParam String categoria,
            @RequestParam Boolean habilitado) {
        return ResponseEntity.ok(service.atualizarPreferencia(clienteId, categoria, habilitado));
    }
}