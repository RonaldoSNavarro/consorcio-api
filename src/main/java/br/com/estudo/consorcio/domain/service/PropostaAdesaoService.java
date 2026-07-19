package br.com.estudo.consorcio.domain.service;

import br.com.estudo.consorcio.domain.dto.PropostaRequestDTO;
import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.Cliente;
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
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import jakarta.transaction.Transactional;
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
                .dataProposta(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();

        return propostaRepository.save(proposta);
    }

    @Transactional
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
        if (hasRestrictedAlerts) {
            throw new RegraDeNegocioException("Venda bloqueada por PLD/FT: Cliente possui alertas restritivos.");
        }

        proposta.setStatus(StatusProposta.APROVADA);
        proposta.setDataAtualizacao(LocalDateTime.now());
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
    public ContratoAdesao efetivarContrato(Long contratoId) {
        ContratoAdesao contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RegraDeNegocioException("Contrato não encontrado"));

        if (contrato.getStatus() != StatusContrato.PENDENTE_PAGAMENTO) {
            throw new RegraDeNegocioException("Contrato deve estar PENDENTE_PAGAMENTO para ser efetivado.");
        }

        boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                contrato.getProposta().getCliente().getId(),
                List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
        );
        if (hasRestrictedAlerts) {
            throw new RegraDeNegocioException("Venda bloqueada por PLD/FT: Cliente possui alertas restritivos.");
        }

        // RN-VND-003: Contrato só gera cota após pagamento
        // Aqui simularíamos o retorno do webhook do banco ou integração com Financeiro
        
        contrato.setStatus(StatusContrato.EFETIVADO);
        contrato.setDataAssinatura(LocalDateTime.now());
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
        
        return contrato;
    }

    private br.com.estudo.consorcio.domain.enums.CategoriaBem mapCategoriaBacen(TipoCategoriaBacen tipoBacen) {
        if (tipoBacen == TipoCategoriaBacen.BEM_IMOVEL) return br.com.estudo.consorcio.domain.enums.CategoriaBem.IMOVEL;
        if (tipoBacen == TipoCategoriaBacen.BEM_MOVEL_I) return br.com.estudo.consorcio.domain.enums.CategoriaBem.VEICULO_AUTOMOTOR;
        if (tipoBacen == TipoCategoriaBacen.BEM_MOVEL_II) return br.com.estudo.consorcio.domain.enums.CategoriaBem.OUTROS_BENS_MOVEIS;
        return br.com.estudo.consorcio.domain.enums.CategoriaBem.SERVICO;
    }
}
