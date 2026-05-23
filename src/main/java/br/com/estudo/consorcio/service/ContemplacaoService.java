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
    private final MovimentoFinanceiroService movimentoService;
    private final CotaService cotaService;
    private final HistoricoConsorciadoService historicoService;

    public ContemplacaoService(ContemplacaoRepository contemplacaoRepository, AssembleiaRepository assembleiaRepository,
                               CotaRepository cotaRepository, ParcelaRepository parcelaRepository,
                               ContemplacaoMapper mapper, MovimentoFinanceiroService movimentoService,
                               CotaService cotaService, HistoricoConsorciadoService historicoService) {
        this.contemplacaoRepository = contemplacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
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
        BigDecimal saldoFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(
                assembleia.getGrupo().getId(),
                StatusParcela.PAGA
        );

        if (saldoFundoComum.compareTo(valorCreditoLiberado) < 0) {
            throw new RegraDeNegocioException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário para liberação: R$ " + valorCreditoLiberado);
        }
        // -------------------------------------------------------- //

        contemplacao.setDataContemplacao(LocalDate.now());

        // 3. Persistência
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        cotaService.registrarTransicaoVersao(cota, StatusCota.CONTEMPLADA, "Cota contemplada na assembleia id " + assembleia.getId());

        // --- Registrar Movimento Financeiro (Módulo 2) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = assembleia.getGrupo();

        if (Boolean.TRUE.equals(contemplacaoSalva.getLanceEmbutido())) {
            movimentoService.registrarMovimento(grupo, cota, null, contemplacaoSalva,
                    TipoMovimentoFinanceiro.LANCE_EMBUTIDO, NaturezaMovimento.CREDITO,
                    contemplacaoSalva.getValorLance(), "Lance embutido utilizado na contemplação - Cota " + cota.getNumeroCota(), usuario);
        }

        movimentoService.registrarMovimento(grupo, cota, null, contemplacaoSalva,
                TipoMovimentoFinanceiro.LIBERACAO_CREDITO, NaturezaMovimento.DEBITO,
                contemplacaoSalva.getValorCreditoLiberado(), "Liberação de carta de crédito na contemplação - Cota " + cota.getNumeroCota(), usuario);

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

        // Regra de segurança: evitar duplicidade de pagamento do bem
        boolean jaPago = movimentoService.listarPorCota(contemplacao.getCota().getId()).stream()
                .anyMatch(mov -> mov.tipoMovimento() == TipoMovimentoFinanceiro.PAGAMENTO_BEM);
        if (jaPago) {
            throw new RegraDeNegocioException("O bem para esta contemplação já foi pago.");
        }

        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = contemplacao.getCota().getGrupo();
        Cota cota = contemplacao.getCota();

        movimentoService.registrarMovimento(grupo, cota, null, contemplacao,
                TipoMovimentoFinanceiro.PAGAMENTO_BEM, NaturezaMovimento.DEBITO,
                contemplacao.getValorCreditoLiberado(), "Pagamento de bem - Cota " + cota.getNumeroCota(), usuario);

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