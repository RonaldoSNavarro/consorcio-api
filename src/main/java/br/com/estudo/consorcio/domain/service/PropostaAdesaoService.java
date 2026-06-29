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
import br.com.estudo.consorcio.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropostaAdesaoService {

    private final PropostaAdesaoRepository propostaRepository;
    private final ContratoAdesaoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoConsorcioRepository produtoRepository;
    private final TipoVendaRepository tipoVendaRepository;
    
    // Injeção dos services reais de Cota/Grupo
    // private final GrupoAlocacaoService grupoAlocacaoService;
    // private final FinanceiroService financeiroService;

    @Transactional
    public PropostaAdesao criarProposta(PropostaRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado"));

        if (cliente.getStatus() != StatusCliente.ATIVO) {
            throw new BusinessException("RN-VND-001: Cliente inativo não pode gerar nova proposta.");
        }

        ProdutoConsorcio produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new BusinessException("Produto não encontrado"));

        TipoVenda tipoVenda = tipoVendaRepository.findById(request.getTipoVendaId())
                .orElseThrow(() -> new BusinessException("Tipo de Venda não encontrado"));

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
                .orElseThrow(() -> new BusinessException("Proposta não encontrada"));

        if (proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new BusinessException("Apenas propostas EM_ANALISE podem ser aprovadas.");
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
                .orElseThrow(() -> new BusinessException("Contrato não encontrado"));

        if (contrato.getStatus() != StatusContrato.PENDENTE_PAGAMENTO) {
            throw new BusinessException("Contrato deve estar PENDENTE_PAGAMENTO para ser efetivado.");
        }

        // RN-VND-003: Contrato só gera cota após pagamento
        // Aqui simularíamos o retorno do webhook do banco ou integração com Financeiro
        
        contrato.setStatus(StatusContrato.EFETIVADO);
        contrato.setDataAssinatura(LocalDateTime.now());
        
        // TODO: grupoAlocacaoService.alocarEmGrupoECriarCota(contrato);
        
        return contratoRepository.save(contrato);
    }
}
