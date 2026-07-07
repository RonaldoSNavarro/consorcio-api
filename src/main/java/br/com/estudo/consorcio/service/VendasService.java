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
import lombok.RequiredArgsConstructor;
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
                .dataProposta(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
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
        proposta.setDataAtualizacao(LocalDateTime.now());
        propostaRepository.save(proposta);

        ContratoAdesao contrato = ContratoAdesao.builder()
                .numeroContrato("CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .proposta(proposta)
                .dataAssinatura(LocalDateTime.now())
                .status(StatusContrato.PENDENTE_PAGAMENTO)
                .build();
        
        contrato = contratoRepository.save(contrato);

        br.com.estudo.consorcio.domain.enums.CategoriaBem catEnum = mapCategoriaBacen(proposta.getProduto().getBemReferencia().getCategoriaBem().getTipoBacen());
        
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
        cota.setCliente(proposta.getCliente());
        cota.setGrupo(grupo);
        cota.setContratoAdesao(contrato);
        cota.setStatus(StatusCota.AGUARDANDO_PAGAMENTO);
        
        cotaRepository.save(cota);

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
                        java.time.LocalDate.now(), 
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
