package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.PerfilRequestDTO;
import br.com.estudo.consorcio.domain.dto.PerfilResponseDTO;
import br.com.estudo.consorcio.domain.model.Permissao;
import br.com.estudo.consorcio.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/perfis")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<List<PerfilResponseDTO>> listarTodos() {
        return ResponseEntity.ok(perfilService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<PerfilResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<PerfilResponseDTO> salvar(@RequestBody @Valid PerfilRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.salvar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<PerfilResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid PerfilRequestDTO dto) {
        return ResponseEntity.ok(perfilService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        perfilService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissoes")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<List<Permissao>> listarPermissoes() {
        return ResponseEntity.ok(Arrays.asList(Permissao.values()));
    }
}
