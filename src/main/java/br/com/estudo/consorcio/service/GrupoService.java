package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.GrupoMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.*;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class GrupoService {

    private final GrupoRepository repository;
    private final ParcelaRepository parcelaRepository;
    private final ContemplacaoRepository contemplacaoRepository;
    private final GrupoMapper mapper;

    public GrupoService(GrupoRepository repository, ParcelaRepository parcelaRepository, ContemplacaoRepository contemplacaoRepository, GrupoMapper mapper) {
        this.repository = repository;
        this.parcelaRepository = parcelaRepository;
        this.contemplacaoRepository = contemplacaoRepository;
        this.mapper = mapper;
    }

    @Transactional
    public GrupoResponseDTO salvar(GrupoRequestDTO dto) {
        // 1. Mapeamento: DTO de entrada para Entidade usando o mapper
        Grupo grupo = mapper.toEntity(dto);

        // Regra BCB: Todo grupo nasce em formação (Garantido pelo Back-end)
        grupo.setStatus(StatusGrupo.EM_FORMACAO);

        // 2. Persistência
        Grupo grupoSalvo = repository.save(grupo);

        // 3. Retorno mapeado para DTO de saída usando o mapper
        return mapper.toResponse(grupoSalvo);
    }

    @Transactional
    public GrupoResponseDTO inaugurar(Long id, LocalDate dataAssembleia) {
        Grupo grupo = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() != StatusGrupo.EM_FORMACAO) {
            throw new RegraDeNegocioException("Apenas grupos em formação podem ser inaugurados.");
        }

        // Regra BCB: O grupo é inaugurado na data da 1ª Assembleia Geral Ordinária (AGO)
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupo.setDataInauguracao(dataAssembleia);

        Grupo grupoInaugurado = repository.save(grupo);

        return mapper.toResponse(grupoInaugurado);
    }

    @Transactional
    public GrupoResponseDTO reajustarGrupo(Long grupoId, BigDecimal novoValorCredito) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível reajustar um grupo já encerrado.");
        }

        if (novoValorCredito == null || novoValorCredito.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor do crédito deve ser maior que zero.");
        }

        BigDecimal antigoValorCredito = grupo.getValorCredito();
        if (novoValorCredito.compareTo(antigoValorCredito) == 0) {
            return mapper.toResponse(grupo);
        }

        // Calcula o fator de reajuste (ex: 110.000 / 100.000 = 1.10)
        BigDecimal fatorReajuste = novoValorCredito.divide(antigoValorCredito, 6, RoundingMode.HALF_UP);

        // Atualiza o valor do crédito do grupo
        grupo.setValorCredito(novoValorCredito);
        Grupo grupoSalvo = repository.save(grupo);

        // Busca e atualiza todas as parcelas PENDENTES ou ATRASADAS das cotas do grupo
        List<StatusParcela> statusesReajustaveis = List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADA);
        List<Parcela> parcelasParaReajustar = parcelaRepository.findByCotaGrupoIdAndStatusIn(grupoId, statusesReajustaveis);

        for (Parcela parcela : parcelasParaReajustar) {
            BigDecimal novoFundoComum = parcela.getValorFundoComum().multiply(fatorReajuste).setScale(2, RoundingMode.HALF_UP);
            parcela.setValorFundoComum(novoFundoComum);
            // O PreUpdate/PrePersist da entidade cuidará de recalcular o total da parcela somando as partes.
        }

        parcelaRepository.saveAll(parcelasParaReajustar);

        return mapper.toResponse(grupoSalvo);
    }

    public GrupoFinanceiroResponseDTO obterRelatorioFinanceiro(Long grupoId) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        // Soma arrecadada de cada componente da parcela paga
        BigDecimal totalFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(grupoId, StatusParcela.PAGA);
        BigDecimal totalTaxaAdmin = parcelaRepository.somarTaxaAdministracaoPorGrupoEStatus(grupoId, StatusParcela.PAGA);
        BigDecimal totalFundoReserva = parcelaRepository.somarFundoReservaPorGrupoEStatus(grupoId, StatusParcela.PAGA);

        // Créditos liberados por contemplação
        BigDecimal totalCreditosLiberados = contemplacaoRepository.somarCreditosLiberadosPorGrupo(grupoId);

        // Saldo disponível
        BigDecimal saldoFC = totalFundoComum.subtract(totalCreditosLiberados);
        if (saldoFC.compareTo(BigDecimal.ZERO) < 0) {
            saldoFC = BigDecimal.ZERO;
        }
        BigDecimal saldoFR = totalFundoReserva;

        return new GrupoFinanceiroResponseDTO(
                grupoId,
                grupo.getCodigo(),
                totalFundoComum,
                totalTaxaAdmin,
                totalFundoReserva,
                totalCreditosLiberados,
                saldoFC,
                saldoFR
        );
    }

    @Transactional
    public GrupoResponseDTO encerrarGrupo(Long grupoId) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Este grupo já está encerrado.");
        }

        // Regra BCB: Verificar se há parcelas pendentes ou atrasadas (inadimplentes) no grupo
        List<StatusParcela> statusesAbertos = List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADA);
        long parcelasAbertas = parcelaRepository.countByCotaGrupoIdAndStatusIn(grupoId, statusesAbertos);

        if (parcelasAbertas > 0) {
            throw new RegraDeNegocioException("Não é possível encerrar o grupo: existem " + parcelasAbertas + " parcelas em aberto.");
        }

        // Altera status para ENCERRADO
        grupo.setStatus(StatusGrupo.ENCERRADO);
        Grupo grupoSalvo = repository.save(grupo);

        return mapper.toResponse(grupoSalvo);
    }

    public List<GrupoResponseDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}