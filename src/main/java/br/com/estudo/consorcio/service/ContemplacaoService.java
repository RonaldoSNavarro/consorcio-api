package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper; // Importar o mapper
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ContemplacaoService {

    private final ContemplacaoRepository contemplacaoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final ContemplacaoMapper mapper; // Injetar o mapper
    private final ContabilidadeService contabilidadeService;
    private final CotaService cotaService;
    private final HistoricoConsorciadoService historicoService;

    public ContemplacaoService(ContemplacaoRepository contemplacaoRepository, AssembleiaRepository assembleiaRepository,
                               CotaRepository cotaRepository, ParcelaRepository parcelaRepository,
                               ContemplacaoMapper mapper, ContabilidadeService contabilidadeService,
                               CotaService cotaService, HistoricoConsorciadoService historicoService) {
        this.contemplacaoRepository = contemplacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
        this.contabilidadeService = contabilidadeService;
        this.cotaService = cotaService;
        this.historicoService = historicoService;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    public ContemplacaoResponseDTO registrar(ContemplacaoRequestDTO dto) {
        // 1. Buscas de Integridade
        Assembleia assembleia = assembleiaRepository.findById(dto.assembleiaId())
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RegraDeNegocioException("A cota e a assembleia pertencem a grupos diferentes.");
        }

        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RegraDeNegocioException("Apenas cotas com status ATIVA podem ser contempladas.");
        }

        // Regra de Compliance Inadimplência: Bloquear contemplação se a cota possuir parcelas vencidas
        boolean possuiParcelasVencidas = parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(
                cota.getId(),
                StatusParcela.PENDENTE,
                assembleia.getDataAssembleia()
        );
        if (possuiParcelasVencidas) {
            throw new RegraDeNegocioException("Não é possível contemplar a cota: existem parcelas em atraso.");
        }

        // 2. Mapeamento inicial usando o mapper
        Contemplacao contemplacao = mapper.toEntity(dto);
        contemplacao.setAssembleia(assembleia); // Setar a assembleia após a busca
        contemplacao.setCota(cota); // Setar a cota após a busca

        BigDecimal valorCreditoGrupo = assembleia.getGrupo().getValorCredito();
        BigDecimal valorCreditoLiberado = valorCreditoGrupo;

        // --- REGRA DO BANCO CENTRAL: LANCE EMBUTIDO DINÂMICO --- //
        if (Boolean.TRUE.equals(contemplacao.getLanceEmbutido())) {

            if (contemplacao.getValorLance() == null || contemplacao.getValorLance().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraDeNegocioException("Para lances embutidos, o valor do lance deve ser informado.");
            }

            BigDecimal limitePercentual = assembleia.getGrupo().getPercentualLanceEmbutidoMaximo();
            BigDecimal limiteEmbutido = valorCreditoGrupo.multiply(limitePercentual).setScale(2, RoundingMode.HALF_UP);

            if (contemplacao.getValorLance().compareTo(limiteEmbutido) > 0) {
                BigDecimal percentualFormatado = limitePercentual.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
                throw new RegraDeNegocioException("O valor do lance embutido não pode ultrapassar " + percentualFormatado + "% do crédito (Máximo permitido: R$ " + limiteEmbutido + ").");
            }

            valorCreditoLiberado = valorCreditoGrupo.subtract(contemplacao.getValorLance());
        }

        contemplacao.setValorCreditoLiberado(valorCreditoLiberado);
        // ---------------------------------------------------------------- //

        // --- REGRA DO BANCO CENTRAL: VALIDAÇÃO DO FUNDO COMUM --- //
        // Extrai o Saldo Real via Double-entry Ledger, consertando o bug anterior
        BigDecimal saldoFundoComum = contabilidadeService.calcularSaldoConta(assembleia.getGrupo(), ContabilidadeService.CONTA_FUNDO_COMUM);

        if (saldoFundoComum.compareTo(valorCreditoLiberado) < 0) {
            throw new RegraDeNegocioException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário para liberação: R$ " + valorCreditoLiberado);
        }
        // -------------------------------------------------------- //

        contemplacao.setDataContemplacao(LocalDate.now());

        // 3. Persistência
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        cotaService.registrarTransicaoVersao(cota, StatusCota.AGUARDANDO_ANALISE, "Cota contemplada na assembleia id " + assembleia.getId() + " - Aguardando Análise de Crédito");

        // --- Registrar Movimento Financeiro (Ledger de Partidas Dobradas) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = assembleia.getGrupo();

        if (Boolean.TRUE.equals(contemplacaoSalva.getLanceEmbutido())) {
            // O lance embutido reduz o crédito e fica no fundo comum. Como o fundo comum nunca chegou a perder esse montante,
            // podemos registrar um estorno contra a provisão ou simplesmente uma retenção se for estritamente contábil.
            contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_FUNDO_COMUM,
                    contemplacaoSalva.getValorLance(), LocalDate.now(), "Lance embutido retido no Fundo Comum - Cota " + cota.getNumeroCota());
        }

        // Movimento de LIBERACAO_CREDITO foi movido para o fluxo de Análise de Crédito (AnaliseCreditoService).

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.CONTEMPLACAO, "Cota contemplada via " + contemplacaoSalva.getTipoContemplacao(),
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                null, null, usuario);

        // 4. Retorno Mapeado usando o mapper
        return mapper.toResponse(contemplacaoSalva);
    }

    @Transactional
    public ContemplacaoResponseDTO pagarBem(Long contemplacaoId) {
        Contemplacao contemplacao = contemplacaoRepository.findById(contemplacaoId)
                .orElseThrow(() -> new RegraDeNegocioException("Contemplação não encontrada."));

        // Regra de segurança simplificada
        if (contemplacao.getValorCreditoLiberado().compareTo(BigDecimal.ZERO) == 0) {
            throw new RegraDeNegocioException("O bem para esta contemplação já foi pago ou o valor é zero.");
        }

        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = contemplacao.getCota().getGrupo();
        Cota cota = contemplacao.getCota();

        // O bem faturado sai do Fundo Comum e vai para Fornecedores ou Caixa (liquidação)
        contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_CAIXA,
                contemplacao.getValorCreditoLiberado(), LocalDate.now(), "Pagamento de bem alienado - Cota " + cota.getNumeroCota());
        
        // Zera o crédito para impedir duplo pagamento
        contemplacao.setValorCreditoLiberado(BigDecimal.ZERO);
        contemplacaoRepository.save(contemplacao);

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.PAGAMENTO_BEM, "Pagamento de bem no valor de R$ " + contemplacao.getValorCreditoLiberado() + " realizado.",
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                "Bem faturado", contemplacao.getValorCreditoLiberado(), usuario);

        return mapper.toResponse(contemplacao);
    }

    public List<ContemplacaoResponseDTO> listarPorAssembleia(Long assembleiaId) {
        return contemplacaoRepository.findByAssembleiaId(assembleiaId).stream()
                .map(mapper::toResponse) // Usar o mapper
                .toList();
    }

    // O método auxiliar converterParaResponseDTO foi removido, pois o mapper faz esse trabalho.
}