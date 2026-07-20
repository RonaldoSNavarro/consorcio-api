package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ComissaoVenda;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.Corretor;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.domain.repository.ComissaoVendaRepository;
import br.com.estudo.consorcio.domain.repository.ContratoAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.PropostaAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendasService {

    private final PropostaAdesaoRepository propostaRepository;
    private final ContratoAdesaoRepository contratoRepository;
    private final GrupoRepository grupoRepository;
    private final CotaRepository cotaRepository;
    private final ComissaoVendaService comissaoService;
    private final ContabilidadeService contabilidadeService;
    private final AssembleiaRepository assembleiaRepository;
    private final ParcelaRepository parcelaRepository;
    private final java.time.Clock clock;

    @Transactional
    public PropostaAdesao criarProposta(Cliente cliente, ProdutoConsorcio produto, TipoVenda tipoVenda, Corretor corretor, BigDecimal valorCredito) {
        PropostaAdesao proposta = PropostaAdesao.builder()
                .numeroProposta("PROP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .cliente(cliente)
                .produto(produto)
                .tipoVenda(tipoVenda)
                .corretor(corretor)
                .valorCreditoSolicitado(valorCredito)
                .status(StatusProposta.EM_ANALISE)
                .dataProposta(LocalDateTime.now(clock))
                .dataAtualizacao(LocalDateTime.now(clock))
                .build();
        return propostaRepository.save(proposta);
    }

    @Transactional
    public ContratoAdesao aprovarProposta(Long propostaId) {
        PropostaAdesao proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada."));

        if (proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new IllegalStateException("Proposta não está em análise.");
        }

        proposta.setStatus(StatusProposta.APROVADA);
        proposta.setDataAtualizacao(LocalDateTime.now(clock));
        propostaRepository.save(proposta);

        ContratoAdesao contrato = ContratoAdesao.builder()
                .numeroContrato("CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .proposta(proposta)
                .dataAssinatura(LocalDateTime.now(clock))
                .status(StatusContrato.PENDENTE_PAGAMENTO)
                .build();
        
        contrato = contratoRepository.save(contrato);

        br.com.estudo.consorcio.domain.enums.CategoriaBem catEnum = mapCategoriaBacen(proposta.getProduto().getBemReferencia().getCategoriaBem().getTipoBacen());
        
        Grupo grupo = grupoRepository.encontrarMelhorGrupoDisponivel(catEnum)
                .orElseThrow(() -> new RegraDeNegocioException("Nenhum grupo com cotas disponíveis encontrado."));

        Cota cota = cotaRepository.findFirstByGrupoIdAndStatusOrderByNumeroCotaAsc(grupo.getId(), StatusCota.DISPONIVEL)
                .orElseThrow(() -> new RegraDeNegocioException("Não há cotas disponíveis neste grupo."));

        cota.setCliente(proposta.getCliente());
        cota.setContratoAdesao(contrato);
        cota.setBemReferencia(proposta.getProduto().getBemReferencia());
        cota.setPrazoMeses(proposta.getProduto().getPrazoMeses());
        cota.setStatus(StatusCota.AGUARDANDO_PAGAMENTO);
        
        cotaRepository.save(cota);

        List<Assembleia> assembleias = assembleiaRepository.findByGrupoIdOrderByDataAssembleiaAsc(grupo.getId());
        List<Parcela> parcelas = new ArrayList<>();
        int numero = 1;
        BigDecimal valorParcela = proposta.getValorCreditoSolicitado().divide(BigDecimal.valueOf(proposta.getProduto().getPrazoMeses()), 2, java.math.RoundingMode.HALF_UP);
        // Simplificado, ideal seria calcular taxa adm e seguro separados
        BigDecimal taxaAdm = valorParcela.multiply(grupo.getTaxaAdministracao()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal fundoComum = valorParcela.subtract(taxaAdm);

        // -- NOVA LÓGICA DE PARCELA 1 AQUI --
        Parcela adesao = new Parcela();
        adesao.setCota(cota);
        adesao.setNumeroParcela(numero++);
        adesao.setDataVencimento(java.time.LocalDate.now(clock));
        adesao.setValorParcela(valorParcela);
        adesao.setValorFundoComum(fundoComum);
        adesao.setValorTaxaAdministracao(taxaAdm);
        adesao.setValorFundoReserva(BigDecimal.ZERO);
        adesao.setValorSeguro(BigDecimal.ZERO);
        adesao.setStatus(StatusParcela.PENDENTE);
        parcelas.add(adesao);

        for (Assembleia assembleia : assembleias) {
            if (numero > proposta.getProduto().getPrazoMeses()) break;
            if (assembleia.getDataAssembleia().isBefore(java.time.LocalDate.now(clock))) continue;

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

    @Transactional
    public void efetivarPagamentoAdesao(Long contratoId) {
        ContratoAdesao contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));

        if (contrato.getStatus() != StatusContrato.PENDENTE_PAGAMENTO) {
            throw new IllegalStateException("Contrato não está aguardando pagamento.");
        }

        contrato.setStatus(StatusContrato.EFETIVADO);
        contratoRepository.save(contrato);

        Cota cota = cotaRepository.findByContratoAdesaoId(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Cota não encontrada para este contrato"));
        
        if (cota.getGrupo().getStatus() == StatusGrupo.EM_FORMACAO) {
            cota.setStatus(StatusCota.AGUARDANDO_INAUGURACAO);
        } else {
            cota.setStatus(StatusCota.ATIVA);
        }
        cotaRepository.save(cota);

        if (contrato.getProposta().getCorretor() != null) {
            BigDecimal comissao = contrato.getProposta().getValorCreditoSolicitado().multiply(contrato.getProposta().getTipoVenda().getPercentualComissao());
            ComissaoVenda com = comissaoService.criarComissaoPendente(contrato.getProposta().getCorretor(), contrato, comissao);

            if (Boolean.TRUE.equals(contrato.getProposta().getTipoVenda().getLiberacaoComissaoImediata())) {
                comissaoService.pagarComissao(com);
                
                // Pagar comissão debitando da Taxa de Adm e creditando no Caixa
                contabilidadeService.registrarBaixa(cota.getGrupo(), cota, null, 
                        br.com.estudo.consorcio.service.ContabilidadeService.CONTA_TAXA_ADM, 
                        br.com.estudo.consorcio.service.ContabilidadeService.CONTA_CAIXA, 
                        comissao, 
                        java.time.LocalDate.now(clock), 
                        "Pagamento imediato de comissão - Contrato " + contrato.getId());
            }
        }
    }

    private br.com.estudo.consorcio.domain.enums.CategoriaBem mapCategoriaBacen(br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen tipoBacen) {
        if (tipoBacen == br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen.BEM_IMOVEL) return br.com.estudo.consorcio.domain.enums.CategoriaBem.IMOVEL;
        if (tipoBacen == br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen.BEM_MOVEL_I) return br.com.estudo.consorcio.domain.enums.CategoriaBem.VEICULO_AUTOMOTOR;
        if (tipoBacen == br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen.BEM_MOVEL_II) return br.com.estudo.consorcio.domain.enums.CategoriaBem.OUTROS_BENS_MOVEIS;
        return br.com.estudo.consorcio.domain.enums.CategoriaBem.SERVICO;
    }
}
