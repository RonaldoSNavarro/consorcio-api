package br.com.estudo.consorcio.domain.service;

import br.com.estudo.consorcio.domain.dto.PropostaRequestDTO;
import br.com.estudo.consorcio.domain.dto.PropostaComplianceResponseDTO;
import br.com.estudo.consorcio.domain.dto.AlertaResumoDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.dto.ProdutoConsorcioResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.AlertaCompliance;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ContratoAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.ProdutoConsorcioRepository;
import br.com.estudo.consorcio.domain.repository.PropostaAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.TipoVendaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropostaAdesaoService {

    private final PropostaAdesaoRepository propostaRepository;
    private final ContratoAdesaoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoConsorcioRepository produtoRepository;
    private final TipoVendaRepository tipoVendaRepository;
    private final AlertaComplianceRepository alertaComplianceRepository;
    
    // Injeção dos repositórios de Cota/Grupo
    private final GrupoRepository grupoRepository;
    private final CotaRepository cotaRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final ParcelaRepository parcelaRepository;
    private final java.time.Clock clock;

    @Transactional
    public PropostaAdesao criarProposta(PropostaRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado"));

        if (cliente.getStatus() != StatusCliente.ATIVO) {
            throw new RegraDeNegocioException("RN-VND-001: Cliente inativo não pode gerar nova proposta.");
        }

        boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                cliente.getId(), 
                List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
        );
        if (hasRestrictedAlerts) {
            throw new RegraDeNegocioException("Venda bloqueada por PLD/FT: Cliente possui alertas restritivos.");
        }

        ProdutoConsorcio produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new RegraDeNegocioException("Produto não encontrado"));

        TipoVenda tipoVenda = tipoVendaRepository.findById(request.getTipoVendaId())
                .orElseThrow(() -> new RegraDeNegocioException("Tipo de Venda não encontrado"));

        PropostaAdesao proposta = PropostaAdesao.builder()
                .numeroProposta("PROP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .cliente(cliente)
                .produto(produto)
                .tipoVenda(tipoVenda)
                .valorCreditoSolicitado(request.getValorCreditoSolicitado())
                .status(StatusProposta.EM_ANALISE)
                .dataProposta(LocalDateTime.now(clock))
                .dataAtualizacao(LocalDateTime.now(clock))
                .build();

        return propostaRepository.save(proposta);
    }

    @Transactional(noRollbackFor = RegraDeNegocioException.class)
    public ContratoAdesao aprovarProposta(Long propostaId) {
        PropostaAdesao proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new RegraDeNegocioException("Proposta não encontrada"));

        if (proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new RegraDeNegocioException("Apenas propostas EM_ANALISE podem ser aprovadas.");
        }

        boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                proposta.getCliente().getId(),
                List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
        );
        if (hasRestrictedAlerts || proposta.getCliente().getNivelRisco() == NivelRisco.ALTO) {
            proposta.setStatus(StatusProposta.PENDENTE_ANALISE_RISCO);
            proposta.setDataAtualizacao(LocalDateTime.now(clock));
            propostaRepository.save(proposta);
            throw new RegraDeNegocioException("Proposta encaminhada para análise manual de risco (Compliance).");
        }


        return efetivarAprovacaoInterna(proposta);
    }

    private ContratoAdesao efetivarAprovacaoInterna(PropostaAdesao proposta) {
        proposta.setStatus(StatusProposta.APROVADA);
        proposta.setDataAtualizacao(LocalDateTime.now(clock));
        propostaRepository.save(proposta);

        ContratoAdesao contrato = ContratoAdesao.builder()
                .numeroContrato("CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .proposta(proposta)
                .dataAssinatura(null) // Assinatura/Aceite virá após pagamento
                .status(StatusContrato.PENDENTE_PAGAMENTO)
                .build();

        contrato = contratoRepository.save(contrato);

        // Gera fatura/boleto simbólico da 1ª parcela da Proposta
        System.out.println("[INFO] Fatura gerada para o contrato: " + contrato.getNumeroContrato());
        
        return contrato;
    }

    @Transactional
    public ContratoAdesao analisarPropostaRisco(Long propostaId, br.com.estudo.consorcio.domain.dto.AnaliseRiscoRequestDTO request) {
        PropostaAdesao proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new RegraDeNegocioException("Proposta não encontrada"));

        if (proposta.getStatus() != StatusProposta.PENDENTE_ANALISE_RISCO) {
            throw new RegraDeNegocioException("Apenas propostas em PENDENTE_ANALISE_RISCO podem ser analisadas no compliance.");
        }

        if (!request.isAprovada()) {
            proposta.setStatus(StatusProposta.REPROVADA_POR_RISCO);
            proposta.setJustificativaReprovacao(request.getJustificativa());
            proposta.setDataAtualizacao(LocalDateTime.now(clock));
            propostaRepository.save(proposta);
            return null;
        }

        ContratoAdesao contrato = efetivarAprovacaoInterna(proposta);
        return efetivarContrato(contrato);
    }

    @Transactional
    public ContratoAdesao efetivarContrato(Long contratoId) {
        ContratoAdesao contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RegraDeNegocioException("Contrato não encontrado"));
        return efetivarContrato(contrato);
    }

    @Transactional
    public ContratoAdesao efetivarContrato(ContratoAdesao contrato) {
        if (contrato == null) {
            throw new RegraDeNegocioException("Contrato não encontrado");
        }

        if (contrato.getStatus() != StatusContrato.PENDENTE_PAGAMENTO) {
            throw new RegraDeNegocioException("Contrato deve estar PENDENTE_PAGAMENTO para ser efetivado.");
        }

        if (contrato.getProposta() == null || contrato.getProposta().getStatus() != StatusProposta.APROVADA) {
            throw new RegraDeNegocioException("Contrato só pode ser efetivado para propostas no status APROVADA.");
        }


        // RN-VND-003: Contrato só gera cota após pagamento
        // Aqui simularíamos o retorno do webhook do banco ou integração com Financeiro
        
        contrato.setStatus(StatusContrato.EFETIVADO);
        contrato.setDataAssinatura(LocalDateTime.now(clock));
        contrato = contratoRepository.save(contrato);
        
        br.com.estudo.consorcio.domain.enums.CategoriaBem catEnum = mapCategoriaBacen(contrato.getProposta().getProduto().getBemReferencia().getCategoriaBem().getTipoBacen());
        
        final br.com.estudo.consorcio.domain.model.PropostaAdesao proposta = contrato.getProposta();
        
        // Alocação Inteligente
        Grupo grupo = grupoRepository.encontrarMelhorGrupoDisponivel(catEnum)
                .orElseGet(() -> {
                    Grupo novo = new Grupo();
                    novo.setCodigo("GRP-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
                    novo.setCategoriaBem(catEnum);
                    novo.setValorCredito(proposta.getValorCreditoSolicitado());
                    novo.setPrazoMeses(proposta.getProduto().getPrazoMeses());
                    novo.setTaxaAdministracao(proposta.getProduto().getTaxaAdministracaoPerc());
                    novo.setStatus(StatusGrupo.EM_FORMACAO);
                    return grupoRepository.save(novo);
                });

        Cota cota = new Cota();
        long cotasNoGrupo = cotaRepository.countByGrupoId(grupo.getId());
        cota.setNumeroCota((int) cotasNoGrupo + 1);
        cota.setCliente(contrato.getProposta().getCliente());
        cota.setGrupo(grupo);
        cota.setContratoAdesao(contrato);
        
        if (grupo.getStatus() == StatusGrupo.EM_FORMACAO) {
            cota.setStatus(StatusCota.AGUARDANDO_INAUGURACAO);
        } else {
            cota.setStatus(StatusCota.ATIVA);
        }
        
        cotaRepository.save(cota);

        // --- GERAÇÃO DE PARCELAS ---
        List<Assembleia> assembleias = assembleiaRepository.findByGrupoIdOrderByDataAssembleiaAsc(grupo.getId());
        java.util.List<Parcela> parcelas = new java.util.ArrayList<>();
        int numero = 1;
        
        BigDecimal prazoTotal = BigDecimal.valueOf(proposta.getProduto().getPrazoMeses());
        BigDecimal valorParcela = proposta.getValorCreditoSolicitado().divide(prazoTotal, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal taxaAdm = valorParcela.multiply(grupo.getTaxaAdministracao()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal fundoComum = valorParcela.subtract(taxaAdm);

        // 1ª Parcela: Adesão (já vencendo hoje)
        Parcela adesao = new Parcela();
        adesao.setCota(cota);
        adesao.setNumeroParcela(numero++);
        adesao.setDataVencimento(LocalDate.now(clock));
        adesao.setValorParcela(valorParcela);
        adesao.setValorFundoComum(fundoComum);
        adesao.setValorTaxaAdministracao(taxaAdm);
        adesao.setValorFundoReserva(BigDecimal.ZERO);
        adesao.setValorSeguro(BigDecimal.ZERO);
        // Se já está efetivando o contrato, a adesão foi paga
        adesao.setStatus(StatusParcela.PAGA);
        adesao.setDataPagamento(LocalDate.now(clock));
        adesao.setValorPago(valorParcela);
        parcelas.add(adesao);

        // Demais parcelas vinculadas às assembleias futuras
        for (Assembleia assembleia : assembleias) {
            if (numero > proposta.getProduto().getPrazoMeses()) break;
            if (assembleia.getDataAssembleia().isBefore(LocalDate.now(clock))) continue;

            Parcela p = new Parcela();
            p.setCota(cota);
            p.setNumeroParcela(numero++);
            p.setDataVencimento(assembleia.getDataAssembleia().minusDays(grupo.getDiasAntecedenciaVencimento()));
            p.setValorParcela(valorParcela);
            p.setValorFundoComum(fundoComum);
            p.setValorTaxaAdministracao(taxaAdm);
            p.setValorFundoReserva(BigDecimal.ZERO);
            p.setValorSeguro(BigDecimal.ZERO);
            p.setStatus(StatusParcela.PENDENTE);
            parcelas.add(p);
        }

        if (!parcelas.isEmpty()) {
            parcelaRepository.saveAll(parcelas);
        }
        
        return contrato;
    }

    private br.com.estudo.consorcio.domain.enums.CategoriaBem mapCategoriaBacen(TipoCategoriaBacen tipoBacen) {
        if (tipoBacen == TipoCategoriaBacen.BEM_IMOVEL) return br.com.estudo.consorcio.domain.enums.CategoriaBem.IMOVEL;
        if (tipoBacen == TipoCategoriaBacen.BEM_MOVEL_I) return br.com.estudo.consorcio.domain.enums.CategoriaBem.VEICULO_AUTOMOTOR;
        if (tipoBacen == TipoCategoriaBacen.BEM_MOVEL_II) return br.com.estudo.consorcio.domain.enums.CategoriaBem.OUTROS_BENS_MOVEIS;
        return br.com.estudo.consorcio.domain.enums.CategoriaBem.SERVICO;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PropostaComplianceResponseDTO> listarPropostasPendentesDeRisco() {
        List<PropostaAdesao> propostas = propostaRepository.findByStatus(StatusProposta.PENDENTE_ANALISE_RISCO);
        
        if (propostas.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        List<Long> clienteIds = propostas.stream().map(p -> p.getCliente().getId()).toList();
        List<AlertaCompliance> todosAlertas = alertaComplianceRepository.findByClienteIdInAndStatusIn(
                clienteIds, 
                List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
        );
        
        java.util.Map<Long, List<AlertaCompliance>> alertasPorCliente = todosAlertas.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getCliente().getId()));

        return propostas.stream()
                .map(proposta -> {
                    List<AlertaCompliance> alertas = alertasPorCliente.getOrDefault(proposta.getCliente().getId(), java.util.Collections.emptyList());
                    
                    List<AlertaResumoDTO> alertasDto = alertas.stream()
                            .map(alerta -> new AlertaResumoDTO(
                                    alerta.getListaRestritiva().getOrigem().name(),
                                    alerta.getListaRestritiva().getNome(),
                                    alerta.getDataDeteccao()
                            ))
                            .toList();
                            
                    Cliente cliente = proposta.getCliente();
                    ClienteResponseDTO clienteDto = new ClienteResponseDTO(
                            cliente.getId(),
                            cliente.getNome(),
                            cliente.getCpfCnpj(),
                            cliente.getEmail(),
                            cliente.getTelefone(),
                            cliente.getCep(),
                            cliente.getLogradouro(),
                            cliente.getNumero(),
                            cliente.getComplemento(),
                            cliente.getBairro(),
                            cliente.getLocalidade(),
                            cliente.getUf(),
                            cliente.getPatrimonio(),
                            cliente.getRendaMensal(),
                            cliente.getNivelRisco(),
                            cliente.getDataCadastro(),
                            cliente.getStatus()
                    );
                    
                    ProdutoConsorcio produto = proposta.getProduto();
                    ProdutoConsorcioResponseDTO produtoDto = new ProdutoConsorcioResponseDTO(
                            produto.getId(),
                            produto.getNome(),
                            produto.getPrazoMeses(),
                            produto.getTaxaAdministracaoPerc()
                    );
                    
                    return new PropostaComplianceResponseDTO(
                            proposta.getId(),
                            proposta.getNumeroProposta(),
                            proposta.getValorCreditoSolicitado(),
                            clienteDto,
                            produtoDto,
                            alertasDto
                    );
                })
                .toList();
    }
}
