package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.BemReferenciaRequestDTO;
import br.com.estudo.consorcio.domain.dto.BemReferenciaResponseDTO;
import br.com.estudo.consorcio.domain.dto.HistoricoValorResponseDTO;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import br.com.estudo.consorcio.domain.model.BemReferencia;
import br.com.estudo.consorcio.domain.model.CategoriaBem;
import br.com.estudo.consorcio.domain.model.HistoricoValorBemReferencia;
import br.com.estudo.consorcio.domain.repository.BemReferenciaRepository;
import br.com.estudo.consorcio.domain.repository.CategoriaBemRepository;
import br.com.estudo.consorcio.domain.repository.HistoricoValorBemReferenciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BemReferenciaService {

    private final BemReferenciaRepository repository;
    private final CategoriaBemRepository categoriaRepository;
    private final HistoricoValorBemReferenciaRepository historicoRepository;

    public BemReferenciaService(
            BemReferenciaRepository repository,
            CategoriaBemRepository categoriaRepository,
            HistoricoValorBemReferenciaRepository historicoRepository
    ) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaBem> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<BemReferenciaResponseDTO> listar(Long categoriaId, Pageable pageable) {
        if (categoriaId != null) {
            return repository.findByCategoriaBemId(categoriaId, pageable).map(this::toResponse);
        }
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BemReferenciaResponseDTO> listarTodosAtivos() {
        return repository.findByAtivoTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BemReferenciaResponseDTO obterPorId(Long id) {
        BemReferencia bem = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Bem de referência não encontrado."));
        return toResponse(bem);
    }

    @Transactional
    public BemReferenciaResponseDTO salvar(BemReferenciaRequestDTO dto) {
        CategoriaBem categoria = categoriaRepository.findById(dto.categoriaBemId())
                .orElseThrow(() -> new RegraDeNegocioException("Categoria do bem não encontrada."));

        BemReferencia bem = BemReferencia.builder()
                .categoriaBem(categoria)
                .descricao(dto.descricao())
                .valorAtual(dto.valorAtual())
                .dataUltimaAtualizacao(LocalDate.now())
                .codigoFipe(dto.codigoFipe())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .build();

        BemReferencia salvo = repository.save(bem);

        // Registrar histórico inicial
        HistoricoValorBemReferencia historico = HistoricoValorBemReferencia.builder()
                .bemReferencia(salvo)
                .valorAnterior(BigDecimal.ZERO)
                .valorNovo(salvo.getValorAtual())
                .origemReajuste("CADASTRO_INICIAL")
                .codigoFipe(salvo.getCodigoFipe())
                .dataAtualizacao(LocalDateTime.now())
                .build();
        historicoRepository.save(historico);

        return toResponse(salvo);
    }

    @Transactional
    public BemReferenciaResponseDTO atualizar(Long id, BemReferenciaRequestDTO dto, String origemReajuste) {
        BemReferencia bem = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Bem de referência não encontrado."));

        CategoriaBem categoria = categoriaRepository.findById(dto.categoriaBemId())
                .orElseThrow(() -> new RegraDeNegocioException("Categoria do bem não encontrada."));

        BigDecimal valorAnterior = bem.getValorAtual();
        BigDecimal valorNovo = dto.valorAtual();

        bem.setCategoriaBem(categoria);
        bem.setDescricao(dto.descricao());
        bem.setValorAtual(valorNovo);
        bem.setDataUltimaAtualizacao(LocalDate.now());
        if (dto.codigoFipe() != null && !dto.codigoFipe().isBlank()) {
            bem.setCodigoFipe(dto.codigoFipe());
        }
        if (dto.ativo() != null) {
            bem.setAtivo(dto.ativo());
        }

        BemReferencia atualizado = repository.save(bem);

        // Registrar no histórico se houver alteração de valor
        if (valorAnterior != null && valorNovo != null && valorAnterior.compareTo(valorNovo) != 0) {
            HistoricoValorBemReferencia historico = HistoricoValorBemReferencia.builder()
                    .bemReferencia(atualizado)
                    .valorAnterior(valorAnterior)
                    .valorNovo(valorNovo)
                    .origemReajuste(origemReajuste != null ? origemReajuste : "MANUAL")
                    .codigoFipe(atualizado.getCodigoFipe())
                    .dataAtualizacao(LocalDateTime.now())
                    .build();
            historicoRepository.save(historico);
        }

        return toResponse(atualizado);
    }

    @Transactional(readOnly = true)
    public List<HistoricoValorResponseDTO> obterHistorico(Long bemReferenciaId) {
        return historicoRepository.findByBemReferenciaIdOrderByDataAtualizacaoDesc(bemReferenciaId)
                .stream()
                .map(h -> new HistoricoValorResponseDTO(
                        h.getId(),
                        h.getBemReferencia().getId(),
                        h.getBemReferencia().getDescricao(),
                        h.getValorAnterior(),
                        h.getValorNovo(),
                        h.getOrigemReajuste(),
                        h.getCodigoFipe(),
                        h.getDataAtualizacao()
                ))
                .toList();
    }

    private BemReferenciaResponseDTO toResponse(BemReferencia bem) {
        return new BemReferenciaResponseDTO(
                bem.getId(),
                bem.getCategoriaBem() != null ? bem.getCategoriaBem().getId() : null,
                bem.getCategoriaBem() != null ? bem.getCategoriaBem().getNome() : null,
                bem.getCategoriaBem() != null && bem.getCategoriaBem().getTipoBacen() != null ? bem.getCategoriaBem().getTipoBacen().name() : null,
                bem.getCategoriaBem() != null && bem.getCategoriaBem().getIndiceReajustePadrao() != null ? bem.getCategoriaBem().getIndiceReajustePadrao().name() : null,
                bem.getDescricao(),
                bem.getValorAtual(),
                bem.getDataUltimaAtualizacao(),
                bem.getCodigoFipe(),
                bem.getAtivo()
        );
    }
}
