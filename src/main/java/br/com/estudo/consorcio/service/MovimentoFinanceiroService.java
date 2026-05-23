package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.MovimentoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.mapper.MovimentoFinanceiroMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.MovimentoFinanceiroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentoFinanceiroService {

    private final MovimentoFinanceiroRepository repository;
    private final MovimentoFinanceiroMapper mapper;

    public MovimentoFinanceiroService(MovimentoFinanceiroRepository repository, MovimentoFinanceiroMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public MovimentoFinanceiro registrarMovimento(
            Grupo grupo, Cota cota, Parcela parcela, Contemplacao contemplacao,
            TipoMovimentoFinanceiro tipoMovimento, NaturezaMovimento natureza,
            BigDecimal valor, String descricao, Usuario usuario) {

        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor do movimento não pode ser nulo ou negativo.");
        }

        // Recupera o último movimento do grupo para calcular os saldos acumulados de forma sequencial
        BigDecimal saldoAnterior = repository.findFirstByGrupoIdOrderByIdDesc(grupo.getId())
                .map(MovimentoFinanceiro::getSaldoPosterior)
                .orElse(BigDecimal.ZERO);

        BigDecimal saldoPosterior;
        if (natureza == NaturezaMovimento.CREDITO) {
            saldoPosterior = saldoAnterior.add(valor);
        } else {
            saldoPosterior = saldoAnterior.subtract(valor);
        }

        MovimentoFinanceiro movimento = MovimentoFinanceiro.builder()
                .grupo(grupo)
                .cota(cota)
                .parcela(parcela)
                .contemplacao(contemplacao)
                .tipoMovimento(tipoMovimento)
                .natureza(natureza)
                .valor(valor)
                .saldoAnterior(saldoAnterior)
                .saldoPosterior(saldoPosterior)
                .descricao(descricao)
                .dataMovimento(LocalDateTime.now())
                .dataReferencia(LocalDate.now())
                .usuario(usuario)
                .build();

        return repository.save(movimento);
    }

    @Transactional(readOnly = true)
    public List<MovimentoFinanceiroResponseDTO> listarPorGrupo(Long grupoId) {
        return repository.findByGrupoIdOrderByDataMovimentoDesc(grupoId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentoFinanceiroResponseDTO> listarPorCota(Long cotaId) {
        return repository.findByCotaIdOrderByDataMovimentoDesc(cotaId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal obterSaldoGrupo(Long grupoId) {
        return repository.calcularSaldoGrupo(grupoId);
    }
}
