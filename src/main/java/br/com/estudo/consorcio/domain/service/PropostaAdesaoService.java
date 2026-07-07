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
    
    // Injeção dos services reais de Cota/Grupo
    // private final GrupoAlocacaoService grupoAlocacaoService;
    // private final FinanceiroService financeiroService;

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

        // TODO: Chamar FinanceiroService para gerar a fatura/boleto da 1ª parcela da Proposta
        
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
        
        // TODO: grupoAlocacaoService.alocarEmGrupoECriarCota(contrato);
        
        return contratoRepository.save(contrato);
    }
}
