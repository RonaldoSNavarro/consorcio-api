package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MotorApuracaoService {

    private final AssembleiaRepository assembleiaRepository;
    private final LanceRepository lanceRepository;
    private final ContabilidadeService contabilidadeService;
    private final ContemplacaoService contemplacaoService;

    public MotorApuracaoService(AssembleiaRepository assembleiaRepository, LanceRepository lanceRepository,
                                ContabilidadeService contabilidadeService, ContemplacaoService contemplacaoService) {
        this.assembleiaRepository = assembleiaRepository;
        this.lanceRepository = lanceRepository;
        this.contabilidadeService = contabilidadeService;
        this.contemplacaoService = contemplacaoService;
    }

    @Transactional
    public void apurarAssembleia(Long assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        if (assembleia.getStatus() != StatusAssembleia.REALIZADA) {
            throw new RegraDeNegocioException("A apuração só pode ocorrer em assembleias com status REALIZADA (captação finalizada).");
        }

        Grupo grupo = assembleia.getGrupo();
        
        // Extrai o Saldo Real do Fundo Comum do Ledger Contábil (Corrigindo o bug da soma isolada)
        BigDecimal saldoFundoComumLivre = contabilidadeService.calcularSaldoConta(grupo, ContabilidadeService.CONTA_FUNDO_COMUM);
        
        // Carrega todos os lances da assembleia
        List<Lance> todosLances = lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(assembleiaId);

        // Segrega em Lances Livres e Lances Fixos
        List<Lance> lancesLivre = todosLances.stream()
                .filter(l -> l.getModalidade() == null || l.getModalidade() == ModalidadeLance.LIVRE)
                .sorted((l1, l2) -> l2.getValorOferta().compareTo(l1.getValorOferta()))
                .toList();

        List<Lance> lancesFixo = todosLances.stream()
                .filter(l -> l.getModalidade() == ModalidadeLance.FIXO)
                .toList();

        // 1. Processar Lances Livres
        for (Lance lance : lancesLivre) {
            BigDecimal valorCredito = grupo.getValorCredito();
            BigDecimal impactoCaixa = valorCredito.subtract(lance.getValorOferta());

            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa); // Abate o saldo consumido
                
                // Registra a Contemplação oficial
                ContemplacaoRequestDTO contemplacaoReq = new ContemplacaoRequestDTO(
                        lance.getCota().getId(),
                        assembleiaId,
                        TipoContemplacao.LANCE_LIVRE,
                        lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO
                );
                
                contemplacaoService.registrar(contemplacaoReq);
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }

        // 2. Processar Lances Fixos com desempate
        List<Lance> lancesFixoOrdenados;
        if (grupo.getCriterioDesempateLance() == CriterioDesempateLance.LOTERIA_FEDERAL) {
            Integer pedra = assembleia.getNumeroSorteado();
            if (pedra == null) {
                pedra = new java.util.Random().nextInt(100) + 1;
                assembleia.setNumeroSorteado(pedra);
                assembleiaRepository.save(assembleia);
            }
            final int pedraFundamental = pedra;
            lancesFixoOrdenados = lancesFixo.stream()
                    .sorted((l1, l2) -> {
                        int dist1 = Math.abs(l1.getCota().getNumeroCota() - pedraFundamental);
                        int dist2 = Math.abs(l2.getCota().getNumeroCota() - pedraFundamental);
                        if (dist1 != dist2) {
                            return Integer.compare(dist1, dist2); // Menor distância primeiro
                        }
                        return Integer.compare(l1.getCota().getNumeroCota(), l2.getCota().getNumeroCota());
                    })
                    .toList();
        } else if (grupo.getCriterioDesempateLance() == CriterioDesempateLance.ORDEM_OFERTA) {
            lancesFixoOrdenados = lancesFixo.stream()
                    .sorted((l1, l2) -> l1.getDataOferta().compareTo(l2.getDataOferta()))
                    .toList();
        } else {
            lancesFixoOrdenados = lancesFixo.stream()
                    .sorted((l1, l2) -> Integer.compare(l1.getCota().getNumeroCota(), l2.getCota().getNumeroCota()))
                    .toList();
        }

        for (Lance lance : lancesFixoOrdenados) {
            BigDecimal valorCredito = grupo.getValorCredito();
            BigDecimal impactoCaixa = valorCredito.subtract(lance.getValorOferta());

            if (saldoFundoComumLivre.compareTo(impactoCaixa) >= 0) {
                lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
                saldoFundoComumLivre = saldoFundoComumLivre.subtract(impactoCaixa); // Abate o saldo consumido
                
                // Registra a Contemplação oficial
                ContemplacaoRequestDTO contemplacaoReq = new ContemplacaoRequestDTO(
                        lance.getCota().getId(),
                        assembleiaId,
                        TipoContemplacao.LANCE_FIXO,
                        lance.getValorOferta(),
                        lance.getTipo() == TipoLance.EMBUTIDO
                );
                
                contemplacaoService.registrar(contemplacaoReq);
            } else {
                lance.setStatusApuracao(StatusApuracaoLance.PERDEDOR);
            }
            lanceRepository.save(lance);
        }

        // Fim da Apuração
        assembleia.setStatus(StatusAssembleia.FECHADA);
        assembleiaRepository.save(assembleia);
    }
}
