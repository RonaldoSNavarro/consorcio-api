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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import br.com.estudo.consorcio.domain.repository.TipoVendaRepository;
import br.com.estudo.consorcio.domain.repository.ProdutoConsorcioRepository;
import java.util.List;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import br.com.estudo.consorcio.domain.dto.TipoVendaRequestDTO;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendasController {

    private final PropostaAdesaoService propostaService;
    private final PropostaAdesaoMapper mapper;
    private final TipoVendaRepository tipoVendaRepository;
    private final ProdutoConsorcioRepository produtoRepository;

    @PreAuthorize("hasAnyAuthority('MANAGE_VENDAS', 'MANAGE_GRUPOS')")
    @GetMapping("/tipos")
    public ResponseEntity<List<TipoVenda>> listarTiposVenda() {
        return ResponseEntity.ok(tipoVendaRepository.findAll());
    }

    @PreAuthorize("hasAnyAuthority('MANAGE_VENDAS', 'MANAGE_GRUPOS')")
    @GetMapping("/tipos/todos")
    public ResponseEntity<List<TipoVenda>> listarTiposTodos() {
        return ResponseEntity.ok(tipoVendaRepository.findAll());
    }

    @PreAuthorize("hasAnyAuthority('MANAGE_VENDAS', 'MANAGE_GRUPOS')")
    @PostMapping("/tipos")
    public ResponseEntity<TipoVenda> criarTipoVenda(@RequestBody @Valid TipoVendaRequestDTO dto) {
        TipoVenda tipo = new TipoVenda();
        tipo.setNome(dto.nome());
        tipo.setDescricao(dto.descricao());
        tipo.setCanal(dto.canal());
        tipo.setPercentualComissao(dto.percentualComissao());
        tipo.setExigeSeguro(dto.exigeSeguro() != null ? dto.exigeSeguro() : false);
        tipo.setPermiteReajuste(dto.permiteReajuste() != null ? dto.permiteReajuste() : true);
        tipo.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoVendaRepository.save(tipo));
    }

    @PreAuthorize("hasAnyAuthority('MANAGE_VENDAS', 'MANAGE_GRUPOS')")
    @PutMapping("/tipos/{id}")
    public ResponseEntity<TipoVenda> atualizarTipoVenda(@PathVariable Long id, @RequestBody @Valid TipoVendaRequestDTO dto) {
        TipoVenda tipo = tipoVendaRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Tipo de venda não encontrado."));
        tipo.setNome(dto.nome());
        tipo.setDescricao(dto.descricao());
        tipo.setCanal(dto.canal());
        tipo.setPercentualComissao(dto.percentualComissao());
        if (dto.exigeSeguro() != null) tipo.setExigeSeguro(dto.exigeSeguro());
        if (dto.permiteReajuste() != null) tipo.setPermiteReajuste(dto.permiteReajuste());
        if (dto.ativo() != null) tipo.setAtivo(dto.ativo());
        return ResponseEntity.ok(tipoVendaRepository.save(tipo));
    }

    @PreAuthorize("hasAnyAuthority('MANAGE_VENDAS', 'MANAGE_GRUPOS')")
    @DeleteMapping("/tipos/{id}")
    public ResponseEntity<Void> inativarTipoVenda(@PathVariable Long id) {
        TipoVenda tipo = tipoVendaRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Tipo de venda não encontrado."));
        tipo.setAtivo(false);
        tipoVendaRepository.save(tipo);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('MANAGE_VENDAS')")
    @GetMapping("/produtos")
    public ResponseEntity<List<ProdutoConsorcio>> listarProdutos() {
        return ResponseEntity.ok(produtoRepository.findAll());
    }

    @PreAuthorize("hasAuthority('MANAGE_VENDAS')")
    @PostMapping("/propostas")
    public ResponseEntity<PropostaResponseDTO> criarProposta(@RequestBody @Valid PropostaRequestDTO request) {
        PropostaAdesao proposta = propostaService.criarProposta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(proposta));
    }

    @PreAuthorize("hasAuthority('MANAGE_VENDAS')")
    @PostMapping("/propostas/{id}/aprovar")
    public ResponseEntity<ContratoResponseDTO> aprovarProposta(@PathVariable Long id) {
        ContratoAdesao contrato = propostaService.aprovarProposta(id);
        return ResponseEntity.ok(mapper.toContratoResponse(contrato));
    }

    @PreAuthorize("hasAuthority('MANAGE_COMPLIANCE')")
    @PostMapping("/propostas/{id}/analise-risco")
    public ResponseEntity<ContratoResponseDTO> analisarRisco(
            @PathVariable Long id, 
            @org.springframework.web.bind.annotation.RequestBody br.com.estudo.consorcio.domain.dto.AnaliseRiscoRequestDTO request) {
        ContratoAdesao contrato = propostaService.analisarPropostaRisco(id, request);
        if (contrato == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(mapper.toContratoResponse(contrato));
    }

    @PreAuthorize("hasAuthority('MANAGE_VENDAS')")
    @PostMapping("/contratos/{id}/efetivar")
    public ResponseEntity<ContratoResponseDTO> efetivarContrato(@PathVariable Long id) {
        ContratoAdesao contrato = propostaService.efetivarContrato(id);
        return ResponseEntity.ok(mapper.toContratoResponse(contrato));
    }

    @PreAuthorize("hasAuthority('MANAGE_COMPLIANCE')")
    @GetMapping("/propostas/pendentes-risco")
    public ResponseEntity<List<br.com.estudo.consorcio.domain.dto.PropostaComplianceResponseDTO>> listarPropostasPendentesDeRisco() {
        return ResponseEntity.ok(propostaService.listarPropostasPendentesDeRisco());
    }
}
