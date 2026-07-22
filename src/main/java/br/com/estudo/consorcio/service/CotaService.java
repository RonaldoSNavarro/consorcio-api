package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaReembolsoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaReembolsoSimulacaoDTO;
import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.mapper.CotaMapper;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.CotaSpecification;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.repository.HistoricoVersaoCotaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import java.time.LocalDate;
import java.util.Optional;
import br.com.estudo.consorcio.domain.dto.HistoricoVersaoCotaResponseDTO;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class CotaService {

    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;
    private final GrupoRepository grupoRepository;
    private final ParcelaRepository parcelaRepository;
    private final CotaMapper mapper;
    private final MovimentoFinanceiroService movimentoService;
    private final HistoricoVersaoCotaRepository historicoVersaoCotaRepository;
    private final HistoricoConsorciadoService historicoService;
    private final ContemplacaoRepository contemplacaoRepository;
    private final ContabilidadeService contabilidadeService;
    private final AlertaComplianceRepository alertaComplianceRepository;
    private final ComissaoVendaService comissaoService;
    private final CorretorService corretorService;

    public CotaService(CotaRepository cotaRepository, ClienteRepository clienteRepository,
                       GrupoRepository grupoRepository, ParcelaRepository parcelaRepository,
                       CotaMapper mapper, MovimentoFinanceiroService movimentoService,
                       HistoricoVersaoCotaRepository historicoVersaoCotaRepository,
                       HistoricoConsorciadoService historicoService,
                       ContemplacaoRepository contemplacaoRepository,
                       ContabilidadeService contabilidadeService,
                       AlertaComplianceRepository alertaComplianceRepository,
                       ComissaoVendaService comissaoService,
                       CorretorService corretorService) {
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.grupoRepository = grupoRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.historicoVersaoCotaRepository = historicoVersaoCotaRepository;
        this.historicoService = historicoService;
        this.contemplacaoRepository = contemplacaoRepository;
        this.contabilidadeService = contabilidadeService;
        this.alertaComplianceRepository = alertaComplianceRepository;
        this.comissaoService = comissaoService;
        this.corretorService = corretorService;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public CotaResponseDTO buscarPorId(Long id) {
        Cota cota = cotaRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));
        return mapper.toResponse(cota);
    }

    @Transactional(readOnly = true)
    public Page<CotaResponseDTO> buscar(Long grupoId, Integer numeroCota, Integer versao, String cpfCnpj, Pageable pageable) {
        Specification<Cota> spec = Specification.where(CotaSpecification.porGrupoId(grupoId))
                .and(CotaSpecification.porNumeroCota(numeroCota))
                .and(CotaSpecification.porVersao(versao))
                .and(CotaSpecification.porCpfCnpj(cpfCnpj))
                .and(CotaSpecification.porStatusDiferenteDe(StatusCota.DISPONIVEL));
        return cotaRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public void registrarTransicaoVersao(Cota cota, StatusCota statusNovo, String motivo) {
        StatusCota statusAnterior = cota.getStatus();

        // A versão inicial de uma cota deve ser 0, pois corresponde a uma cota em situação normal.
        // A versão 1 corresponde a uma cota excluída.
        if (statusNovo == StatusCota.EXCLUIDA) {
            cota.setVersao(1);
        }

        cota.setStatus(statusNovo);
        cotaRepository.save(cota);

        Usuario usuario = getUsuarioAutenticado();

        HistoricoVersaoCota historico = HistoricoVersaoCota.builder()
                .cota(cota)
                .versao(cota.getVersao())
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .motivo(motivo)
                .usuario(usuario)
                .build();

        historicoVersaoCotaRepository.save(historico);
    }

    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'VIEW_COMPLIANCE') or @ownershipGuard.canAccessCota(#cotaId)")
    public List<HistoricoVersaoCotaResponseDTO> listarVersoes(Long cotaId) {
        // Garantir que a cota existe
        if (!cotaRepository.existsById(cotaId)) {
            throw new RegraDeNegocioException("Cota não encontrada.");
        }
        return historicoVersaoCotaRepository.findByCotaIdOrderByDataTransicaoDesc(cotaId).stream()
                .map(entity -> new HistoricoVersaoCotaResponseDTO(
                        entity.getId(),
                        entity.getCota().getId(),
                        entity.getVersao(),
                        entity.getStatusAnterior(),
                        entity.getStatusNovo(),
                        entity.getMotivo(),
                        entity.getDataTransicao(),
                        entity.getUsuario() != null ? entity.getUsuario().getUsername() : null
                ))
                .toList();
    }

    @Transactional
    public CotaResponseDTO salvar(CotaRequestDTO dto) {
        // 1. Busca as entidades reais no banco de dados garantindo que elas existem
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado."));

        // Regra de Compliance LGPD/Negócio: Impedir que clientes inativos comprem novas cotas
        validarClienteAtivo(cliente);

        // Bloqueio PLD/FT: Verifica se cliente está em listas restritivas (OFAC/ONU/PEP)
        boolean hasAlertaRestritivo = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                cliente.getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
        if (hasAlertaRestritivo) {
            throw new RegraDeNegocioException("Operação bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
        }

        Grupo grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        // Regra BACEN: Consorciado não pode ter mais de 10% das cotas ativas do grupo
        if (grupo.getPrazoMeses() != null && grupo.getPrazoMeses() > 0) {
            long cotasAtivasDoCliente = cotaRepository.findByGrupoId(grupo.getId(), Pageable.unpaged()).stream()
                    .filter(c -> c.getCliente() != null && c.getCliente().getId().equals(cliente.getId()) && c.getStatus() == StatusCota.ATIVA)
                    .count();
            
            // Verifica o limite de 10% (arredondado para baixo ou limite fixo)
            long limite = (long) (grupo.getPrazoMeses() * 0.10);
            if (limite == 0) limite = 1; // Se grupo tem menos de 10 cotas, pode ter ao menos 1

            if (cotasAtivasDoCliente >= limite) {
                throw new RegraDeNegocioException("Limite excedido: Consorciado não pode possuir mais que 10% das cotas ativas do grupo (Limite atual: " + limite + ").");
            }
        }

        // 2. Mapeamento para a Entidade usando o mapper
        Cota cota = mapper.toEntity(dto);
        cota.setCliente(cliente);
        cota.setGrupo(grupo);

        // A cota nasce AGUARDANDO_INAUGURACAO se o grupo estiver EM_FORMACAO, e ATIVA se EM_ANDAMENTO
        if (grupo.getStatus() == br.com.estudo.consorcio.domain.model.StatusGrupo.EM_FORMACAO) {
            cota.setStatus(StatusCota.AGUARDANDO_INAUGURACAO);
        } else {
            cota.setStatus(StatusCota.ATIVA);
        }

        // 3. Persistência
        Cota cotaSalva = cotaRepository.save(cota);

        // 4. Retorno mapeado usando o mapper
        return mapper.toResponse(cotaSalva);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_COTAS')")
    public CotaResponseDTO cancelarCota(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() == StatusCota.CANCELADA) {
            throw new RegraDeNegocioException("Esta cota já está cancelada.");
        }

        // Altera status para CANCELADA usando a transição controlada
        registrarTransicaoVersao(cota, StatusCota.CANCELADA, "Cota cancelada pelo administrador.");

        // Remove todas as parcelas PENDENTES (ou atrasadas/abertas) da cota
        List<Parcela> parcelasPendentes = parcelaRepository.findByCotaId(cotaId).stream()
                .filter(p -> p.getStatus() == StatusParcela.PENDENTE)
                .toList();

        parcelaRepository.deleteAll(parcelasPendentes);

        // --- Registrar Interação de Histórico (Módulo 4) ---
        Usuario usuario = getUsuarioAutenticado();
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.CANCELAMENTO_COTA, "Cota cancelada no grupo.",
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                null, null, usuario);

        // --- Estorno / Clawback de Comissão (Regra de Garantia) ---
        if (cota.getContratoAdesao() != null && cota.getContratoAdesao().getProposta() != null 
            && cota.getContratoAdesao().getProposta().getTipoVenda() != null) {
            br.com.estudo.consorcio.domain.model.ContratoAdesao contrato = cota.getContratoAdesao();
            br.com.estudo.consorcio.domain.model.TipoVenda tipoVenda = contrato.getProposta().getTipoVenda();
            
            if (tipoVenda.getMesesGarantiaComissao() != null && tipoVenda.getPercentualEstorno() != null) {
                long parcelasPagasCount = parcelaRepository.findByCotaId(cotaId).stream()
                        .filter(p -> p.getStatus() == StatusParcela.PAGA)
                        .count();
                
                if (parcelasPagasCount < tipoVenda.getMesesGarantiaComissao()) {
                    comissaoService.buscarPorContratoEStatus(contrato.getId(), "PAGA").ifPresent(comissao -> {
                        BigDecimal valorEstorno = comissao.getValorTotalComissao().multiply(tipoVenda.getPercentualEstorno());
                        
                        br.com.estudo.consorcio.domain.model.Corretor corretor = comissao.getCorretor();
                        corretorService.adicionarDividaClawback(corretor, valorEstorno);
                        
                        comissaoService.estornarComissao(comissao);
                        
                        // Ledger: debitar contas a receber (do corretor) e creditar Taxa Adm
                        contabilidadeService.registrarBaixa(cota.getGrupo(), cota, null,
                                ContabilidadeService.CONTA_DIREITOS_RECEBER,
                                ContabilidadeService.CONTA_TAXA_ADM,
                                valorEstorno,
                                java.time.LocalDate.now(),
                                "Clawback de comissão - Cancelamento antes da garantia. Contrato " + contrato.getId());
                    });
                }
            }
        }

        return mapper.toResponse(cota);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_COTAS', 'MANAGE_FINANCEIRO')")
    public CotaReembolsoResponseDTO reembolsarCota(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() != StatusCota.CANCELADA) {
            throw new RegraDeNegocioException("Apenas cotas canceladas/excluídas podem ser reembolsadas.");
        }

        if (Boolean.TRUE.equals(cota.getReembolsada())) {
            throw new RegraDeNegocioException("Esta cota já foi reembolsada.");
        }

        Optional<Contemplacao> contemplacaoOpt = contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(cotaId);

        BigDecimal totalFundoComumPago;
        BigDecimal multaRescisoria;
        BigDecimal valorReembolsado;

        if (contemplacaoOpt.isPresent()) {
            Contemplacao contemplacao = contemplacaoOpt.get();
            // O valor líquido já está fixado na contemplação
            valorReembolsado = contemplacao.getValorCreditoLiberado();
            
            // Recalcula o valor bruto correspondente a esse valor líquido
            // valorReembolsado = valorBruto * 0.90 -> valorBruto = valorReembolsado / 0.90
            BigDecimal valorBruto = valorReembolsado.divide(new BigDecimal("0.90"), 2, RoundingMode.HALF_UP);
            multaRescisoria = valorBruto.subtract(valorReembolsado).setScale(2, RoundingMode.HALF_UP);
            totalFundoComumPago = valorBruto;
        } else {
            // Fallback: calcula percentual amortizado e multiplica pelo valor atual do bem
            List<Parcela> parcelasPagas = parcelaRepository.findByCotaId(cotaId).stream()
                    .filter(p -> p.getStatus() == StatusParcela.PAGA)
                    .toList();

            BigDecimal percentualAmortizado = parcelasPagas.stream()
                    .map(p -> p.getPercentualFundoComum() != null ? p.getPercentualFundoComum() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal valorBem = cota.getGrupo().getValorCredito();
            if (valorBem != null && valorBem.compareTo(BigDecimal.ZERO) > 0) {
                totalFundoComumPago = percentualAmortizado.multiply(valorBem).setScale(2, RoundingMode.HALF_UP);
            } else {
                totalFundoComumPago = BigDecimal.ZERO;
            }

            multaRescisoria = totalFundoComumPago.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            valorReembolsado = totalFundoComumPago.subtract(multaRescisoria).setScale(2, RoundingMode.HALF_UP);
        }

        // Atualiza a cota
        cota.setValorReembolsado(valorReembolsado);
        cota.setReembolsada(true);
        cotaRepository.save(cota);

        // --- Registrar Movimentos Financeiros no Ledger (Double-Entry) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = cota.getGrupo();

        if (valorReembolsado.compareTo(BigDecimal.ZERO) > 0) {
            // Se contemplado: sai do passivo de excluídos para o Caixa. Se fallback: do Fundo Comum direto.
            String contaDebito = contemplacaoOpt.isPresent() ? ContabilidadeService.CONTA_EXCLUIDOS_DEVOLVER : ContabilidadeService.CONTA_FUNDO_COMUM;

            contabilidadeService.registrarBaixa(grupo, cota, null, contaDebito, ContabilidadeService.CONTA_CAIXA,
                    valorReembolsado, LocalDate.now(), "Desembolso de reembolso de excluído - Cota " + cota.getNumeroCota());

            movimentoService.registrarMovimento(grupo, cota, null, null,
                    TipoMovimentoFinanceiro.REEMBOLSO, NaturezaMovimento.DEBITO,
                    valorReembolsado, "Reembolso de cota cancelada - Cota " + cota.getNumeroCota(), usuario);
        }

        if (multaRescisoria.compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, null, null,
                    TipoMovimentoFinanceiro.MULTA_RESCISORIA, NaturezaMovimento.CREDITO,
                    multaRescisoria, "Multa rescisória de cota cancelada - Cota " + cota.getNumeroCota(), usuario);
            
            // Se não foi contemplada, precisamos contabilizar a retenção da multa rescisória no momento do reembolso final.
            // (Para as contempladas, a multa já foi retida na assembleia de contemplação).
            if (contemplacaoOpt.isEmpty()) {
                String contaDestinoMulta = (grupo.getDestinacaoMultaRescisoria() != null && grupo.getDestinacaoMultaRescisoria() == br.com.estudo.consorcio.domain.enums.DestinacaoMultaRescisoria.TAXA_ADMINISTRACAO)
                        ? ContabilidadeService.CONTA_TAXA_ADM
                        : ContabilidadeService.CONTA_FUNDO_RESERVA;

                contabilidadeService.registrarBaixa(
                        grupo, cota, null,
                        ContabilidadeService.CONTA_FUNDO_COMUM,
                        contaDestinoMulta,
                        multaRescisoria,
                        LocalDate.now(),
                        "Multa rescisória retida no encerramento - Cota " + cota.getNumeroCota()
                );
            }
        }

        // --- Registrar Interação de Histórico ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.REEMBOLSO, "Reembolso efetuado para a cota cancelada. Valor bruto FC: R$ " + totalFundoComumPago + " | Multa: R$ " + multaRescisoria + " | Reembolsado: R$ " + valorReembolsado,
                cota.getGrupo().getValorCredito(), totalFundoComumPago,
                null, null, null,
                null, null, usuario);

        return new CotaReembolsoResponseDTO(
                cotaId,
                cota.getNumeroCota(),
                totalFundoComumPago,
                multaRescisoria,
                valorReembolsado,
                true
        );
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'VIEW_COMPLIANCE')")
    public Page<CotaResponseDTO> listarTodas(Pageable pageable) {
        return cotaRepository.findByStatusNot(StatusCota.DISPONIVEL, pageable)
                .map(mapper::toResponse);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'VIEW_COMPLIANCE') or @ownershipGuard.canAccessCliente(#clienteId)")
    public Page<CotaResponseDTO> listarPorCliente(Long clienteId, Pageable pageable) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado."));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            boolean hasGlobalAccess = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("VIEW_COTAS") || 
                                   a.getAuthority().equals("MANAGE_COTAS") || 
                                   a.getAuthority().equals("ROLE_ADMIN"));

            if (!hasGlobalAccess && !username.equals("admin") && !username.equals(cliente.getCpfCnpj()) && !username.equals(cliente.getEmail())) {
                throw new AccessDeniedException("Acesso negado. Você só pode acessar seus próprios dados.");
            }
        }

        return cotaRepository.findByClienteId(clienteId, pageable)
                .map(mapper::toResponse);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_COTAS', 'VIEW_COMPLIANCE')")
    public Page<CotaResponseDTO> listarPorGrupo(Long grupoId, Pageable pageable) {
        return cotaRepository.findByGrupoId(grupoId, pageable)
                .map(mapper::toResponse);
    }

    private void validarClienteAtivo(Cliente cliente) {
        if (StatusCliente.INATIVO.equals(cliente.getStatus())) {
            throw new ClienteInativoException(
                    "Operação não permitida: cliente id " + cliente.getId() + " está inativo.");
        }
    }

    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_COTAS')")
    public List<CotaReembolsoSimulacaoDTO> listarPendentesReembolso() {
        List<Cota> cotas = cotaRepository.findByStatusAndReembolsadaFalse(StatusCota.CANCELADA);
        return cotas.stream().map(cota -> {
            Optional<Contemplacao> contemplacaoOpt = contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(cota.getId());
            
            BigDecimal valorBruto = BigDecimal.ZERO;
            BigDecimal multa = BigDecimal.ZERO;
            BigDecimal valorLiquido = BigDecimal.ZERO;
            BigDecimal percentualPago = BigDecimal.ZERO;
            BigDecimal valorBem = cota.getGrupo().getValorCredito();
            LocalDate dataContemplacao = null;
            String numAssembleia = null;

            if (contemplacaoOpt.isPresent()) {
                Contemplacao contemplacao = contemplacaoOpt.get();
                valorLiquido = contemplacao.getValorCreditoLiberado();
                valorBruto = valorLiquido.divide(new BigDecimal("0.90"), 2, RoundingMode.HALF_UP);
                multa = valorBruto.subtract(valorLiquido).setScale(2, RoundingMode.HALF_UP);
                dataContemplacao = contemplacao.getDataContemplacao();
                numAssembleia = contemplacao.getAssembleia() != null ? String.valueOf(contemplacao.getAssembleia().getId()) : "N/A";
            } else {
                List<Parcela> parcelasPagas = parcelaRepository.findByCotaId(cota.getId()).stream()
                        .filter(p -> p.getStatus() == StatusParcela.PAGA)
                        .toList();
                
                BigDecimal percentualAmortizado = parcelasPagas.stream()
                        .map(p -> p.getPercentualFundoComum() != null ? p.getPercentualFundoComum() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (valorBem != null && valorBem.compareTo(BigDecimal.ZERO) > 0) {
                    valorBruto = percentualAmortizado.multiply(valorBem).setScale(2, RoundingMode.HALF_UP);
                } else {
                    valorBruto = BigDecimal.ZERO;
                }
                multa = valorBruto.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                valorLiquido = valorBruto.subtract(multa).setScale(2, RoundingMode.HALF_UP);
            }
            
            if (valorBem != null && valorBem.compareTo(BigDecimal.ZERO) > 0) {
                percentualPago = valorBruto.multiply(new BigDecimal("100")).divide(valorBem, 2, RoundingMode.HALF_UP);
            }
            
            BigDecimal valorHistoricoPago = parcelaRepository.findByCotaId(cota.getId()).stream()
                        .filter(p -> p.getStatus() == StatusParcela.PAGA)
                        .map(Parcela::getValorFundoComum)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new CotaReembolsoSimulacaoDTO(
                    cota.getId(),
                    cota.getNumeroCota(),
                    cota.getCliente().getId(),
                    cota.getCliente().getNome(),
                    cota.getCliente().getCpfCnpj(),
                    numAssembleia,
                    dataContemplacao,
                    valorBem,
                    percentualPago,
                    valorHistoricoPago,
                    valorBruto,
                    multa,
                    valorLiquido
            );
        }).toList();
    }

    @Transactional
    public CotaResponseDTO transferirCota(Long cotaId, br.com.estudo.consorcio.domain.dto.TransferirCotaRequestDTO dto) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() == StatusCota.CANCELADA || cota.getStatus() == StatusCota.EXCLUIDA) {
            throw new RegraDeNegocioException("Não é possível transferir uma cota cancelada ou excluída.");
        }

        Cliente novoCliente = clienteRepository.findById(dto.novoClienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Novo cliente não encontrado."));

        validarClienteAtivo(novoCliente);

        // Bloqueio PLD/FT: Verifica se cliente destino está em listas restritivas
        boolean hasAlertaRestritivo = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                novoCliente.getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
        if (hasAlertaRestritivo) {
            throw new RegraDeNegocioException("Transferência bloqueada pelo Compliance. Cliente destino possui alertas restritivos (PLD/FT).");
        }

        // Bloqueio PLD/FT: Verifica se cliente origem (cedente) está em listas restritivas
        if (cota.getCliente() != null) {
            boolean hasAlertaRestritivoOrigem = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                    cota.getCliente().getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
            if (hasAlertaRestritivoOrigem) {
                throw new RegraDeNegocioException("Transferência bloqueada pelo Compliance. Cliente de origem possui alertas restritivos (PLD/FT).");
            }
        }

        if (cota.getCliente() != null && cota.getCliente().getId().equals(novoCliente.getId())) {
            throw new RegraDeNegocioException("O novo cliente deve ser diferente do cliente atual.");
        }

        // Valida limite de 10%
        long cotasAtivasDoCliente = cotaRepository.findByGrupoId(cota.getGrupo().getId(), Pageable.unpaged()).stream()
                .filter(c -> c.getCliente().getId().equals(novoCliente.getId()) && c.getStatus() == StatusCota.ATIVA)
                .count();
        
        long limite = (long) (cota.getGrupo().getPrazoMeses() * 0.10);
        if (limite == 0) limite = 1;

        if (cotasAtivasDoCliente >= limite) {
            throw new RegraDeNegocioException("Transferência negada: Novo titular excederia o limite de 10% das cotas ativas do grupo.");
        }

        Cliente clienteAnterior = cota.getCliente();
        cota.setCliente(novoCliente);

        // Registro de versão para auditoria (mesmo status, muda o titular e versão++)
        cota.setVersao(cota.getVersao() + 1);
        cotaRepository.save(cota);
        
        Usuario usuario = getUsuarioAutenticado();

        HistoricoVersaoCota historico = HistoricoVersaoCota.builder()
                .cota(cota)
                .versao(cota.getVersao())
                .statusAnterior(cota.getStatus())
                .statusNovo(cota.getStatus())
                .motivo("Transferência de titularidade de " + clienteAnterior.getNome() + " para " + novoCliente.getNome() + ". Motivo: " + dto.motivo())
                .usuario(usuario)
                .build();
        historicoVersaoCotaRepository.save(historico);

        if (dto.taxaTransferencia() != null && dto.taxaTransferencia().compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(cota.getGrupo(), cota, null, null,
                    TipoMovimentoFinanceiro.TAXA_TRANSFERENCIA, NaturezaMovimento.CREDITO,
                    dto.taxaTransferencia(), "Taxa de transferência de cota cobrada.", usuario);
        }

        historicoService.registrarInteracao(novoCliente, cota, cota.getGrupo(), null, TipoInteracao.CESSAO_DIREITOS, "Transferência de titularidade efetuada com sucesso.", cota.getGrupo().getValorCredito(), null, null, null, null, null, null, usuario);

        return mapper.toResponse(cota);
    }

    @Transactional
    public CotaResponseDTO readmitirCota(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() != StatusCota.EXCLUIDA) {
            throw new RegraDeNegocioException("Apenas cotas EXCLUIDAS podem ser readmitidas.");
        }

        if (cota.getReembolsada() != null && cota.getReembolsada()) {
            throw new RegraDeNegocioException("Cota já reembolsada não pode ser readmitida.");
        }
        
        Optional<Contemplacao> contemplacaoOpt = contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(cotaId);
        if (contemplacaoOpt.isPresent()) {
            throw new RegraDeNegocioException("Cota contemplada não pode ser readmitida sob esta regra.");
        }

        // Bloqueio PLD/FT: Verifica se cliente possui alertas restritivos
        if (cota.getCliente() != null) {
            boolean hasAlertaRestritivoReadmissao = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                    cota.getCliente().getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
            if (hasAlertaRestritivoReadmissao) {
                throw new RegraDeNegocioException("Readmissão bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
            }
        }

        // Valida se as parcelas atrasadas foram pagas/regularizadas
        boolean possuiAtraso = parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(
                cotaId, StatusParcela.PENDENTE, LocalDate.now());
        if (possuiAtraso) {
            throw new RegraDeNegocioException("A cota possui parcelas em atraso e não pode ser readmitida sem regularização.");
        }

        // Valida limite de 10%
        long cotasAtivasDoCliente = 0;
        if (cota.getCliente() != null) {
            cotasAtivasDoCliente = cotaRepository.findByGrupoId(cota.getGrupo().getId(), Pageable.unpaged()).stream()
                    .filter(c -> c.getCliente() != null && c.getCliente().getId().equals(cota.getCliente().getId()) && c.getStatus() == StatusCota.ATIVA)
                    .count();
        }
        
        long limite = (long) (cota.getGrupo().getPrazoMeses() * 0.10);
        if (limite == 0) limite = 1;

        if (cotasAtivasDoCliente >= limite) {
            throw new RegraDeNegocioException("Readmissão negada: Cliente excederia o limite de 10% das cotas ativas do grupo.");
        }

        cota.setStatus(StatusCota.ATIVA);
        cota.setVersao(cota.getVersao() + 1);
        cotaRepository.save(cota);

        Usuario usuario = getUsuarioAutenticado();

        HistoricoVersaoCota historico = HistoricoVersaoCota.builder()
                .cota(cota)
                .versao(cota.getVersao())
                .statusAnterior(StatusCota.EXCLUIDA)
                .statusNovo(StatusCota.ATIVA)
                .motivo("Readmissão de consorciado excluído (Art. 31-A BCB 285).")
                .usuario(usuario)
                .build();
        historicoVersaoCotaRepository.save(historico);

        historicoService.registrarInteracao(cota.getCliente(), cota, cota.getGrupo(), null, TipoInteracao.OUTROS, "Readmissão de consorciado excluído efetuada com sucesso.", cota.getGrupo().getValorCredito(), null, null, null, null, null, null, usuario);

        return mapper.toResponse(cota);
    }
}