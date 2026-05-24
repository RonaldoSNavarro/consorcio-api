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
        
        // Lances já ordenados do maior para o menor (ordem de sorteio)
        List<Lance> lances = lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(assembleiaId);

        // Algoritmo de cruzamento de lances vs caixa
        for (Lance lance : lances) {
            BigDecimal valorCredito = grupo.getValorCredito();
            
            // O impacto no caixa: O Consórcio entrega o Crédito, mas o Lance fica retido no fundo.
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

        // Fim da Apuração
        assembleia.setStatus(StatusAssembleia.FECHADA);
        assembleiaRepository.save(assembleia);
    }
}
