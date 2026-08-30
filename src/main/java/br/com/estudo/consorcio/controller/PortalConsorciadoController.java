package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.dto.PortalCotaDTO;
import br.com.estudo.consorcio.domain.dto.PortalExtratoItemDTO;
import br.com.estudo.consorcio.domain.dto.PortalOfertaLanceDTO;
import br.com.estudo.consorcio.service.PortalConsorciadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portal")
@Tag(name = "Portal do Consorciado", description = "Endpoints de autoatendimento para consulta de cotas, extrato e oferta de lances")
public class PortalConsorciadoController {

    private final PortalConsorciadoService service;

    public PortalConsorciadoController(PortalConsorciadoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar cotas do consorciado logado")
    @PreAuthorize("hasAnyRole('CONSORCIADO', 'ADMIN', 'CLIENTE') or hasAnyAuthority('VIEW_COTAS', 'ROLE_CONSORCIADO')")
    @GetMapping("/minhas-cotas")
    public ResponseEntity<List<PortalCotaDTO>> listarMinhasCotas(
            @RequestParam(required = false) String cpfCnpj,
            Authentication authentication) {
        String doc = resolverDocumentoSeguro(cpfCnpj, authentication);
        return ResponseEntity.ok(service.listarCotasDoCliente(doc));
    }

    @Operation(summary = "Obter extrato detalhado de parcelas da cota")
    @PreAuthorize("hasAnyRole('CONSORCIADO', 'ADMIN', 'CLIENTE') or hasAnyAuthority('VIEW_COTAS', 'ROLE_CONSORCIADO')")
    @GetMapping("/cota/{cotaId}/extrato")
    public ResponseEntity<List<PortalExtratoItemDTO>> obterExtrato(
            @PathVariable Long cotaId,
            @RequestParam(required = false) String cpfCnpj,
            Authentication authentication) {
        String doc = resolverDocumentoSeguro(cpfCnpj, authentication);
        return ResponseEntity.ok(service.obterExtratoCota(cotaId, doc));
    }

    @Operation(summary = "Ofertar lance online no portal")
    @PreAuthorize("hasAnyRole('CONSORCIADO', 'ADMIN', 'CLIENTE') or hasAnyAuthority('VIEW_COTAS', 'ROLE_CONSORCIADO')")
    @PostMapping("/ofertar-lance")
    public ResponseEntity<LanceResponseDTO> ofertarLance(
            @Valid @RequestBody PortalOfertaLanceDTO dto,
            @RequestParam(required = false) String cpfCnpj,
            Authentication authentication) {
        String doc = resolverDocumentoSeguro(cpfCnpj, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ofertarLanceOnline(dto, doc));
    }

    private String resolverDocumentoSeguro(String cpfCnpj, Authentication authentication) {
        Authentication auth = (authentication != null) ? authentication : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String usuarioLogado = (auth != null) ? auth.getName() : null;

        if (cpfCnpj == null || cpfCnpj.isBlank() || (usuarioLogado != null && usuarioLogado.equals(cpfCnpj))) {
            return (usuarioLogado != null) ? usuarioLogado : cpfCnpj;
        }

        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("MANAGE_CLIENTES"));

        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Acesso negado: você não tem permissão para consultar ou operar cotas de outro titular."
            );
        }

        return cpfCnpj;
    }
}