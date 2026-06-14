package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AlertaPldFtResponseDTO;
import br.com.estudo.consorcio.domain.dto.BalanceteResponseDTO;
import br.com.estudo.consorcio.domain.dto.ContaSaldoDTO;
import br.com.estudo.consorcio.domain.dto.EstatisticasGrupoResponseDTO;
import br.com.estudo.consorcio.domain.model.ContaContabil;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.Lance;
import br.com.estudo.consorcio.domain.model.StatusApuracaoLance;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.TipoContemplacao;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private final GrupoRepository grupoRepository;
    private final ContabilidadeService contabilidadeService;
    private final CotaRepository cotaRepository;
    private final LanceRepository lanceRepository;
    private final ContemplacaoRepository contemplacaoRepository;

    private static final BigDecimal LIMITE_PLD_FT = new BigDecimal("50000.00");

    public RelatorioService(GrupoRepository grupoRepository,
                            ContabilidadeService contabilidadeService,
                            CotaRepository cotaRepository,
                            LanceRepository lanceRepository,
                            ContemplacaoRepository contemplacaoRepository) {
        this.grupoRepository = grupoRepository;
        this.contabilidadeService = contabilidadeService;
        this.cotaRepository = cotaRepository;
        this.lanceRepository = lanceRepository;
        this.contemplacaoRepository = contemplacaoRepository;
    }

    @Transactional(readOnly = true)
    public BalanceteResponseDTO gerarBalancete(Long grupoId, LocalDate dataReferencia) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new br.com.estudo.consorcio.exception.RecursoNaoEncontradoException("Grupo não encontrado com ID: " + grupoId));

        List<ContaContabil> contas = contabilidadeService.listarTodasContas();

        List<ContaSaldoDTO> saldos = contas.stream().map(conta -> {
            BigDecimal saldo = contabilidadeService.calcularSaldoConta(grupo, conta.getCodigoCosif());
            return new ContaSaldoDTO(
                    conta.getCodigoCosif(),
                    conta.getNome(),
                    conta.getNatureza().name(),
                    saldo
            );
        }).collect(Collectors.toList());

        return new BalanceteResponseDTO(
                grupo.getId(),
                grupo.getCodigo(),
                dataReferencia,
                saldos
        );
    }

    @Transactional(readOnly = true)
    public EstatisticasGrupoResponseDTO gerarEstatisticas(Long grupoId, LocalDate dataInicio, LocalDate dataFim) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new br.com.estudo.consorcio.exception.RecursoNaoEncontradoException("Grupo não encontrado com ID: " + grupoId));

        // Note: Adesões e exclusões are tracked by status in current snapshot or we can just query Cota status.
        // For accurate statistics, a real history table would be better, but we will use the existing queries.
        long totalAdesoes = cotaRepository.countByGrupoId(grupoId); // simplistic
        long totalExclusoes = cotaRepository.countByGrupoIdAndStatus(grupoId, StatusCota.CANCELADA);

        LocalDateTime inicioTs = dataInicio.atStartOfDay();
        LocalDateTime fimTs = dataFim.atTime(23, 59, 59);

        long totalLancesOfertados = lanceRepository.countByGrupoIdAndPeriodo(grupoId, inicioTs, fimTs);
        long totalLancesVencedores = lanceRepository.countByGrupoIdAndStatusAndPeriodo(grupoId, StatusApuracaoLance.VENCEDOR, inicioTs, fimTs);

        long totalContemplacoesSorteio = contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(grupoId, TipoContemplacao.SORTEIO, dataInicio, dataFim);
        long totalContemplacoesLanceLivre = contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(grupoId, TipoContemplacao.LANCE_LIVRE, dataInicio, dataFim);
        long totalContemplacoesLanceFixo = contemplacaoRepository.countByGrupoIdAndTipoAndPeriodo(grupoId, TipoContemplacao.LANCE_FIXO, dataInicio, dataFim);

        BigDecimal valorTotalCreditosLiberados = contemplacaoRepository.somarCreditosLiberadosPorGrupoEPeriodo(grupoId, dataInicio, dataFim);

        return new EstatisticasGrupoResponseDTO(
                grupo.getId(),
                grupo.getCodigo(),
                dataInicio,
                dataFim,
                totalAdesoes,
                totalExclusoes,
                totalLancesOfertados,
                totalLancesVencedores,
                totalContemplacoesSorteio,
                (totalContemplacoesLanceLivre + totalContemplacoesLanceFixo),
                valorTotalCreditosLiberados
        );
    }

    @Transactional(readOnly = true)
    public List<AlertaPldFtResponseDTO> gerarAlertaPldFt(LocalDateTime dataInicio, LocalDateTime dataFim) {
        List<Lance> lancesSuspeitos = lanceRepository.findByValorOfertaGreaterThanEqualAndDataOfertaBetween(
                LIMITE_PLD_FT, dataInicio, dataFim
        );

        return lancesSuspeitos.stream().map(lance -> new AlertaPldFtResponseDTO(
                lance.getId(),
                lance.getCota().getId(),
                lance.getCota().getCliente().getNome(),
                lance.getCota().getCliente().getCpfCnpj(),
                lance.getValorOferta(),
                lance.getTipo().name(),
                lance.getDataOferta(),
                lance.getAssembleia().getGrupo().getId(),
                lance.getAssembleia().getGrupo().getCodigo()
        )).collect(Collectors.toList());
    }
}
