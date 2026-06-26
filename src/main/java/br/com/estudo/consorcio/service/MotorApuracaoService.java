package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ApuracaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Motor de apuração das assembleias.
 * Responsável pela contemplação de lances (livre e fixo) e pelo sorteio
 * entre cotas ATIVAS e CANCELADAS.
 * Regras:
 * - Cotas CANCELADAS participam do sorteio para fins de restituição (BCB).
 * - Sorteio suporta Loteria Federal (dezena do prêmio principal) e Pedra Chave.
 */
@Slf4j
@Service
public class MotorApuracaoService {

    private final AssembleiaRepository assembleiaRepository;
    private final LanceRepository lanceRepository;
    private final CotaRepository cotaRepository;
    private final ContabilidadeService contabilidadeService;
    private final ContemplacaoService contemplacaoService;

    public MotorApuracaoService(AssembleiaRepository assembleiaRepository,
                                LanceRepository lanceRepository,
                                CotaRepository cotaRepository,
                                ContabilidadeService contabilidadeService,
                                ContemplacaoService contemplacaoService) {
        this.assembleiaRepository = assembleiaRepository;
        this.lanceRepository = lanceRepository;
        this.cotaRepository = cotaRepository;
        this.contabilidadeService = contabilidadeService;
        this.contemplacaoService = contemplacaoService;
    }

    /**
     * Apura uma assembleia completa: lances + sorteio.
     * Se dto não for nulo, usa os parâmetros externos (dezenas da Loteria Federal).
     * Se dto for nulo, usa random como fallback.
     */
    @Transactional
    public void apurarAssembleia(Long assembleiaId) {
        apurarAssembleia(assembleiaId, null);
    }

    @Transactional
    public void apurarAssembleia(Long assembleiaId, ApuracaoRequestDTO params) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        if (assembleia.getStatus() != StatusAssembleia.REALIZADA) {
            throw new RegraDeNegocioException("A apuração só pode ocorrer em assembleias com status REALIZADA.");
        }

        Grupo grupo = assembleia.getGrupo();
        BigDecimal saldoFundoComumLivre = contabilidadeService.calcularSaldoConta(grupo, ContabilidadeService.CONTA_FUNDO_COMUM);

        // ═══════════════════════════════════════════════
        // FASE 1: Apuração de Lances (Livre e Fixo)
        // ═══════════════════════════════════════════════
        List<Lance> todosLances = lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(assembleiaId);

        List<Lance> lancesLivre = todosLances.stream()
                .filter(l -> l.getModalidade() == null || l.getModalidade() == ModalidadeLance.LIVRE)
                .sorted((l1, l2) -> {
                    int cmp = l2.getValorOferta().compareTo(l1.getValorOferta());
                    if (cmp != 0) return cmp;
                    return criarComparadorDesempate(grupo, assembleia, params).compare(l1, l2);
                })
                .toList();

        List<Lance> lancesFixo = todosLances.stream()
                .filter(l -> l.getModalidade() == ModalidadeLance.FIXO)
                .toList();

        // IDs de cotas já contempladas nesta assembleia (para excluir do sorteio)
        List<Long> cotasJaContempladas = new ArrayList<>();

        for (Lance lance : lancesLivre) {
            BigDecimal valorCredito = grupo.getValorCredito();
            BigDecimal impactoCaixa = valorCredito.subtract(lance.getValorOferta());
            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa);
                contemplacaoService.registrar(new ContemplacaoRequestDTO(
                        lance.getCota().getId(), assembleiaId,
                        TipoContemplacao.LANCE_LIVRE, lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO));
                cotasJaContempladas.add(lance.getCota().getId());
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }

        List<Lance> lancesFixoOrdenados = ordenarLancesFixos(lancesFixo, grupo, assembleia, params);
        for (Lance lance : lancesFixoOrdenados) {
            BigDecimal valorCredito = grupo.getValorCredito();
            BigDecimal impactoCaixa = valorCredito.subtract(lance.getValorOferta());
            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa);
                contemplacaoService.registrar(new ContemplacaoRequestDTO(
                        lance.getCota().getId(), assembleiaId,
                        TipoContemplacao.LANCE_FIXO, lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO));
                cotasJaContempladas.add(lance.getCota().getId());
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }

        // ═══════════════════════════════════════════════
        // FASE 2: Sorteio (ATIVAS + CANCELADAS)
        // BCB: cotas canceladas participam para fins de restituição
        // ═══════════════════════════════════════════════
        if (params != null && Boolean.TRUE.equals(params.realizarSorteio())) {
            realizarSorteio(assembleia, grupo, saldoFundoComumLivre, cotasJaContempladas, params);
        }

        // Fim da Apuração
        assembleia.setStatus(StatusAssembleia.FECHADA);
        assembleiaRepository.save(assembleia);
        log.info("Assembleia {} apurada e fechada com sucesso.", assembleiaId);
    }

    private void realizarSorteio(Assembleia assembleia, Grupo grupo,
                                  BigDecimal saldoDisponivel,
                                  List<Long> cotasJaContempladas,
                                  ApuracaoRequestDTO params) {
        // Carrega cotas ATIVAS e CANCELADAS (excluindo já contempladas)
        List<Cota> cotasElegiveis = cotaRepository.findByGrupoId(grupo.getId()).stream()
                .filter(c -> (c.getStatus() == StatusCota.ATIVA || c.getStatus() == StatusCota.CANCELADA))
                .filter(c -> !cotasJaContempladas.contains(c.getId()))
                .toList();

        if (cotasElegiveis.isEmpty()) {
            log.info("Nenhuma cota elegível para sorteio na assembleia {}.", assembleia.getId());
            return;
        }

        // Determinar a cota sorteada pelo número mais próximo à dezena
        int dezena = resolverDezena(assembleia, params);
        log.info("Assembleia {}: Dezena utilizada no sorteio = {}.", assembleia.getId(), dezena);

        // Seleciona a cota cujo numeroCota é mais próximo da dezena
        Cota cotaSorteada = cotasElegiveis.stream()
                .min((c1, c2) -> {
                    int d1 = Math.abs(c1.getNumeroCota() - dezena);
                    int d2 = Math.abs(c2.getNumeroCota() - dezena);
                    if (d1 != d2) return Integer.compare(d1, d2);
                    return Integer.compare(c1.getNumeroCota(), c2.getNumeroCota()); // menor cota como desempate
                })
                .orElse(null);

        if (cotaSorteada == null) return;

        // Verifica se há saldo para contemplar (mesmo que cancelada: deve ter fundo para restituição)
        if (saldoDisponivel.compareTo(BigDecimal.ZERO) > 0) {
            contemplacaoService.registrar(new ContemplacaoRequestDTO(
                    cotaSorteada.getId(),
                    assembleia.getId(),
                    TipoContemplacao.SORTEIO,
                    BigDecimal.ZERO,
                    false));
            log.info("Sorteio: cota {} contemplada na assembleia {}.",
                    cotaSorteada.getNumeroCota(), assembleia.getId());
        } else {
            log.warn("Saldo insuficiente para contemplação por sorteio na assembleia {}.", assembleia.getId());
        }
    }

    private int resolverDezena(Assembleia assembleia, ApuracaoRequestDTO params) {
        // Prioridade: dezena informada > número já salvo na assembleia > random
        if (params != null && params.dezenaSorteio() != null && params.dezenaSorteio() > 0) {
            assembleia.setNumeroSorteado(params.dezenaSorteio());
            assembleiaRepository.save(assembleia);
            return params.dezenaSorteio();
        }
        if (assembleia.getNumeroSorteado() != null) {
            return assembleia.getNumeroSorteado();
        }
        int dezena = new Random().nextInt(100) + 1;
        assembleia.setNumeroSorteado(dezena);
        assembleiaRepository.save(assembleia);
        return dezena;
    }

    private java.util.Comparator<Lance> criarComparadorDesempate(Grupo grupo, Assembleia assembleia, ApuracaoRequestDTO params) {
        if (grupo.getCriterioDesempateLance() == CriterioDesempateLance.LOTERIA_FEDERAL) {
            int pedra = resolverDezena(assembleia, params);
            return (l1, l2) -> {
                int d1 = Math.abs(l1.getCota().getNumeroCota() - pedra);
                int d2 = Math.abs(l2.getCota().getNumeroCota() - pedra);
                if (d1 != d2) return Integer.compare(d1, d2);
                return Integer.compare(l1.getCota().getNumeroCota(), l2.getCota().getNumeroCota());
            };
        } else if (grupo.getCriterioDesempateLance() == CriterioDesempateLance.ORDEM_OFERTA) {
            return (l1, l2) -> l1.getDataOferta().compareTo(l2.getDataOferta());
        } else {
            return (l1, l2) -> Integer.compare(l1.getCota().getNumeroCota(), l2.getCota().getNumeroCota());
        }
    }

    private List<Lance> ordenarLancesFixos(List<Lance> lancesFixo, Grupo grupo,
                                            Assembleia assembleia, ApuracaoRequestDTO params) {
        return lancesFixo.stream()
                .sorted(criarComparadorDesempate(grupo, assembleia, params))
                .toList();
    }
}
