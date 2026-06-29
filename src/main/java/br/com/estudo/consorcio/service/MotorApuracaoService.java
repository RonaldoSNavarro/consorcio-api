package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ApuracaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.domain.util.PedraChaveCalculator;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de apuração das assembleias.
 * Responsável pela contemplação de lances (livre e fixo) e pelo sorteio
 * entre cotas ATIVAS e CANCELADAS.
 * Regras (Resolução BCB 285):
 * - Ordem obrigatória: 1. Sorteio Ativos -> 2. Sorteio Excluídos -> 3. Lances
 * - Sorteio suporta Loteria Federal (Pedra Chave) e direções de fallback.
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
        List<Long> cotasJaContempladas = new ArrayList<>();

        boolean realizarSorteio = (params == null || Boolean.TRUE.equals(params.realizarSorteio()));
        int numeroPremio = resolverNumeroPremio(assembleia, params);

        Cota cotaAtivaSorteada = null;
        // ═══════════════════════════════════════════════
        // FASE 1: Sorteio Ativos
        // ═══════════════════════════════════════════════
        if (realizarSorteio) {
            cotaAtivaSorteada = realizarSorteioAtivos(assembleia, grupo, saldoFundoComumLivre, cotasJaContempladas, numeroPremio);
            if (cotaAtivaSorteada != null) {
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(grupo.getValorCredito());
            }
        }

        // ═══════════════════════════════════════════════
        // FASE 2: Sorteio Excluídos (Restituição)
        // ═══════════════════════════════════════════════
        if (realizarSorteio && cotaAtivaSorteada != null) {
            saldoFundoComumLivre = realizarSorteioExcluidos(assembleia, grupo, saldoFundoComumLivre, cotasJaContempladas, cotaAtivaSorteada.getNumeroCota());
        }

        // ═══════════════════════════════════════════════
        // FASE 3: Apuração de Lances (Livre e Fixo)
        // ═══════════════════════════════════════════════
        apurarLances(assembleia, grupo, saldoFundoComumLivre, cotasJaContempladas, numeroPremio);

        // Fim da Apuração
        assembleia.setStatus(StatusAssembleia.FECHADA);
        assembleiaRepository.save(assembleia);
        log.info("Assembleia {} apurada e fechada com sucesso.", assembleiaId);
    }

    private Cota realizarSorteioAtivos(Assembleia assembleia, Grupo grupo,
                                       BigDecimal saldoDisponivel,
                                       List<Long> cotasJaContempladas,
                                       int numeroPremio) {
        if (saldoDisponivel.compareTo(grupo.getValorCredito()) < 0) {
            log.warn("Saldo insuficiente para contemplação por sorteio de ativos na assembleia {}.", assembleia.getId());
            return null;
        }

        List<Cota> cotasAtivas = cotaRepository.findByGrupoId(grupo.getId()).stream()
                .filter(c -> c.getStatus() == StatusCota.ATIVA)
                .filter(c -> !cotasJaContempladas.contains(c.getId()))
                .toList();

        if (cotasAtivas.isEmpty()) return null;

        int totalCotasAtivas = cotasAtivas.size();
        int pedraChave = PedraChaveCalculator.calcular(grupo.getAlgoritmoPedraChave(), numeroPremio, totalCotasAtivas);
        log.info("Assembleia {}: Prêmio = {}, Pedra Chave Ativos = {}", assembleia.getId(), numeroPremio, pedraChave);
        
        // Auditoria
        assembleia.setAlgoritmoUsado(grupo.getAlgoritmoPedraChave());
        assembleia.setNumeroExtracaoLoteria(String.valueOf(numeroPremio));
        assembleia.setPedraChaveCalculada(pedraChave);

        Cota cotaSorteada = buscarCotaApta(pedraChave, cotasAtivas, grupo.getDirecaoFallbackSorteio(), assembleia);

        if (cotaSorteada != null) {
            contemplacaoService.registrar(new ContemplacaoRequestDTO(
                    cotaSorteada.getId(), assembleia.getId(), TipoContemplacao.SORTEIO, BigDecimal.ZERO, false));
            cotasJaContempladas.add(cotaSorteada.getId());
            log.info("Sorteio Ativos: cota {} contemplada na assembleia {}.", cotaSorteada.getNumeroCota(), assembleia.getId());
            return cotaSorteada;
        }
        return null;
    }

    private BigDecimal realizarSorteioExcluidos(Assembleia assembleia, Grupo grupo,
                                             BigDecimal saldoDisponivel,
                                             List<Long> cotasJaContempladas,
                                             int numeroCotaAlvo) {
        // Simplificação: apenas garantir saldo > 0.
        if (saldoDisponivel.compareTo(BigDecimal.ZERO) <= 0) {
            return saldoDisponivel;
        }

        // Canceladas ou Excluídas
        List<Cota> cotasExcluidas = cotaRepository.findByGrupoId(grupo.getId()).stream()
                .filter(c -> c.getStatus() == StatusCota.CANCELADA || c.getStatus() == StatusCota.EXCLUIDA)
                .filter(c -> !cotasJaContempladas.contains(c.getId()))
                .filter(c -> c.getVersao() != null && c.getVersao() > 0)
                .toList();

        if (cotasExcluidas.isEmpty()) return saldoDisponivel;

        log.info("Assembleia {}: Alvo Excluídos = cota {}", assembleia.getId(), numeroCotaAlvo);

        Cota cotaSorteada = buscarCotaApta(numeroCotaAlvo, cotasExcluidas, grupo.getDirecaoFallbackSorteio(), assembleia);

        if (cotaSorteada != null) {
            // Valor da restituição deve ser calculado, aqui usamos ZERO como DTO simplificado, contabilidade resolve
            contemplacaoService.registrar(new ContemplacaoRequestDTO(
                    cotaSorteada.getId(), assembleia.getId(), TipoContemplacao.SORTEIO, BigDecimal.ZERO, false));
            cotasJaContempladas.add(cotaSorteada.getId());
            log.info("Sorteio Excluídos: cota {} (versão {}) contemplada para restituição na assembleia {}.", 
                     cotaSorteada.getNumeroCota(), cotaSorteada.getVersao(), assembleia.getId());
            // No mundo real, deduziriamos o valor exato da restituição do saldo disponível
        }
        return saldoDisponivel;
    }

    private void apurarLances(Assembleia assembleia, Grupo grupo,
                              BigDecimal saldoFundoComumLivre,
                              List<Long> cotasJaContempladas,
                              int numeroPremio) {

        List<Lance> todosLances = lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(assembleia.getId());

        List<Lance> lancesLivre = todosLances.stream()
                .filter(l -> l.getModalidade() == null || l.getModalidade() == ModalidadeLance.LIVRE)
                .sorted((l1, l2) -> {
                    int cmp = l2.getValorOferta().compareTo(l1.getValorOferta());
                    if (cmp != 0) return cmp;
                    return criarComparadorDesempate(grupo, numeroPremio).compare(l1, l2);
                })
                .toList();

        List<Lance> lancesFixo = todosLances.stream()
                .filter(l -> l.getModalidade() == ModalidadeLance.FIXO)
                .sorted(criarComparadorDesempate(grupo, numeroPremio))
                .toList();

        for (Lance lance : lancesLivre) {
            if (cotasJaContempladas.contains(lance.getCota().getId())) {
                lance.setStatusApuracao(StatusApuracaoLance.INVALIDO); // Já contemplada no sorteio
                lanceRepository.save(lance);
                continue;
            }

            BigDecimal impactoCaixa = grupo.getValorCredito().subtract(lance.getValorOferta());
            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa);
                contemplacaoService.registrar(new ContemplacaoRequestDTO(
                        lance.getCota().getId(), assembleia.getId(),
                        TipoContemplacao.LANCE_LIVRE, lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO));
                cotasJaContempladas.add(lance.getCota().getId());
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }

        for (Lance lance : lancesFixo) {
             if (cotasJaContempladas.contains(lance.getCota().getId())) {
                lance.setStatusApuracao(StatusApuracaoLance.INVALIDO);
                lanceRepository.save(lance);
                continue;
            }

            BigDecimal impactoCaixa = grupo.getValorCredito().subtract(lance.getValorOferta());
            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa);
                contemplacaoService.registrar(new ContemplacaoRequestDTO(
                        lance.getCota().getId(), assembleia.getId(),
                        TipoContemplacao.LANCE_FIXO, lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO));
                cotasJaContempladas.add(lance.getCota().getId());
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }
    }

    private Cota buscarCotaApta(int pedraChave, List<Cota> cotas, DirecaoFallbackSorteio direcao, Assembleia assembleia) {
        Set<Integer> numerosAptos = cotas.stream().map(Cota::getNumeroCota).collect(Collectors.toSet());
        if (numerosAptos.isEmpty()) return null;

        int min = numerosAptos.stream().mapToInt(Integer::intValue).min().orElse(1);
        int max = numerosAptos.stream().mapToInt(Integer::intValue).max().orElse(1);

        List<Integer> candidatas = new ArrayList<>();

        if (direcao == DirecaoFallbackSorteio.ACIMA_DEPOIS_ABAIXO) {
            for (int i = pedraChave; i <= max; i++) candidatas.add(i);
            for (int i = min; i < pedraChave; i++) candidatas.add(i);
        } else if (direcao == DirecaoFallbackSorteio.ABAIXO_DEPOIS_ACIMA) {
            for (int i = pedraChave; i >= min; i--) candidatas.add(i);
            for (int i = max; i > pedraChave; i--) candidatas.add(i);
        } else if (direcao == DirecaoFallbackSorteio.SO_ACIMA) {
            for (int i = pedraChave; i <= max; i++) candidatas.add(i);
        } else if (direcao == DirecaoFallbackSorteio.SO_ABAIXO) {
            for (int i = pedraChave; i >= min; i--) candidatas.add(i);
        }

        int fallbacks = 0;
        for (int num : candidatas) {
            if (numerosAptos.contains(num)) {
                if (assembleia.getFallbacksAplicados() == null) {
                    assembleia.setFallbacksAplicados(fallbacks);
                } else {
                    assembleia.setFallbacksAplicados(assembleia.getFallbacksAplicados() + fallbacks);
                }
                return cotas.stream().filter(c -> c.getNumeroCota() == num).findFirst().orElse(null);
            }
            fallbacks++;
        }
        return null;
    }

    private int resolverNumeroPremio(Assembleia assembleia, ApuracaoRequestDTO params) {
        if (params != null && params.dezenaSorteio() != null && params.dezenaSorteio() > 0) {
            assembleia.setNumeroSorteado(params.dezenaSorteio());
            return params.dezenaSorteio();
        }
        if (assembleia.getNumeroSorteado() != null) {
            return assembleia.getNumeroSorteado();
        }
        int dezena = new Random().nextInt(100000) + 1; // Até 5 dígitos (Loteria Federal)
        assembleia.setNumeroSorteado(dezena);
        return dezena;
    }

    private java.util.Comparator<Lance> criarComparadorDesempate(Grupo grupo, int numeroPremio) {
        if (grupo.getCriterioDesempateLance() == CriterioDesempateLance.LOTERIA_FEDERAL ||
            grupo.getCriterioDesempateLance() == CriterioDesempateLance.PROXIMIDADE_COTA_SORTEADA) {
            // Em caso de empate, usar proximidade à pedra-chave simulada (usando divisão 1000 para lances)
            int pedra = PedraChaveCalculator.calcular(AlgoritmoPedraChave.DIVISAO_1000, numeroPremio, 1000); 
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
}
