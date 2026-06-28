package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.TipoVendaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TipoVendaResponseDTO;
import br.com.estudo.consorcio.domain.dto.VendaPropostaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.*;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsável pela gestão de Tipos de Venda e pela efetivação
 * de vendas de proposta de adesão (criação automática de Cota).
 */
@Service
public class VendaPropostaService {

    private final br.com.estudo.consorcio.domain.repository.TipoVendaRepository tipoVendaRepository;
    private final GrupoRepository grupoRepository;
    private final ClienteRepository clienteRepository;
    private final CotaRepository cotaRepository;
    private final CotaService cotaService;
    private final HistoricoConsorciadoService historicoService;
    private final ListaRestritivaRepository listaRestritivaRepository;

    public VendaPropostaService(
            br.com.estudo.consorcio.domain.repository.TipoVendaRepository tipoVendaRepository,
            GrupoRepository grupoRepository,
            ClienteRepository clienteRepository,
            CotaRepository cotaRepository,
            CotaService cotaService,
            HistoricoConsorciadoService historicoService,
            ListaRestritivaRepository listaRestritivaRepository) {
        this.tipoVendaRepository = tipoVendaRepository;
        this.grupoRepository = grupoRepository;
        this.clienteRepository = clienteRepository;
        this.cotaRepository = cotaRepository;
        this.cotaService = cotaService;
        this.historicoService = historicoService;
        this.listaRestritivaRepository = listaRestritivaRepository;
    }

    // --- CRUD de Tipos de Venda ---

    public List<TipoVendaResponseDTO> listarTiposVenda() {
        return tipoVendaRepository.findByAtivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TipoVendaResponseDTO> listarTodosTiposVenda() {
        return tipoVendaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TipoVendaResponseDTO criarTipoVenda(TipoVendaRequestDTO dto) {
        if (tipoVendaRepository.existsByNome(dto.nome())) {
            throw new RegraDeNegocioException("Já existe um tipo de venda com o nome '" + dto.nome() + "'.");
        }
        TipoVenda tv = new TipoVenda();
        tv.setNome(dto.nome());
        tv.setDescricao(dto.descricao());
        tv.setCanal(dto.canal());
        tv.setPercentualComissao(dto.percentualComissao());
        tv.setExigeSeguro(dto.exigeSeguro() != null ? dto.exigeSeguro() : false);
        tv.setPermiteReajuste(dto.permiteReajuste() != null ? dto.permiteReajuste() : true);
        tv.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return toResponse(tipoVendaRepository.save(tv));
    }

    @Transactional
    public TipoVendaResponseDTO atualizarTipoVenda(Long id, TipoVendaRequestDTO dto) {
        TipoVenda tv = tipoVendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de Venda não encontrado."));
        tv.setNome(dto.nome());
        tv.setDescricao(dto.descricao());
        tv.setCanal(dto.canal());
        tv.setPercentualComissao(dto.percentualComissao());
        if (dto.exigeSeguro() != null) tv.setExigeSeguro(dto.exigeSeguro());
        if (dto.permiteReajuste() != null) tv.setPermiteReajuste(dto.permiteReajuste());
        if (dto.ativo() != null) tv.setAtivo(dto.ativo());
        return toResponse(tipoVendaRepository.save(tv));
    }

    @Transactional
    public void inativarTipoVenda(Long id) {
        TipoVenda tv = tipoVendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de Venda não encontrado."));
        tv.setAtivo(false);
        tipoVendaRepository.save(tv);
    }

    // --- Efetivação de Venda de Proposta ---

    @Transactional
    public CotaResponseDTO efetivarVenda(VendaPropostaRequestDTO dto) {
        // 1. Valida entidades
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));

        Grupo grupo = grupoRepository.findAll().stream()
                .filter(g -> (g.getStatus() == StatusGrupo.EM_FORMACAO || g.getStatus() == StatusGrupo.EM_ANDAMENTO))
                .filter(g -> g.getValorCredito().compareTo(dto.valorCreditoDesejado()) == 0)
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Nenhum grupo disponível para o valor de crédito desejado."));

        TipoVenda tipoVenda = tipoVendaRepository.findById(dto.tipoVendaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de Venda não encontrado."));

        if (!tipoVenda.getAtivo()) {
            throw new RegraDeNegocioException("O tipo de venda '" + tipoVenda.getNome() + "' está inativo.");
        }

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível vender uma proposta em um grupo encerrado.");
        }

        if (cliente.getStatus() != null && cliente.getStatus() == StatusCliente.INATIVO) {
            throw new RegraDeNegocioException("Não é possível vender uma proposta para um cliente inativo.");
        }

        // Compliance PLD/FT: Block OFAC and ONU
        String nomeNormalizado = normalizar(cliente.getNome());
        if (listaRestritivaRepository.existsByNomeAndOrigem(nomeNormalizado, OrigemListaRestritiva.OFAC) ||
            listaRestritivaRepository.existsByNomeAndOrigem(nomeNormalizado, OrigemListaRestritiva.ONU)) {
            throw new RegraDeNegocioException("Venda bloqueada por PLD/FT: Cliente consta em lista restritiva internacional (OFAC/ONU).");
        }

        // 2. Calcula próximo número de cota
        long totalCotas = cotaRepository.countByGrupoId(grupo.getId());
        int proximoNumero = (int) totalCotas + 1;

        // 3. Delega criação da cota ao CotaService (que já valida, persiste e gera parcelas)
        CotaRequestDTO cotaRequest = new CotaRequestDTO(
                proximoNumero, dto.clienteId(), grupo.getId());
        CotaResponseDTO cotaResponse = cotaService.salvar(cotaRequest);

        // 4. Registra histórico
        historicoService.registrarInteracao(
                cliente, null, grupo, null,
                TipoInteracao.GERACAO_PARCELAS,
                "Proposta de adesão efetivada via canal: " + tipoVenda.getCanal().name()
                + " | Tipo: " + tipoVenda.getNome()
                + (Boolean.TRUE.equals(dto.contratarSeguro()) ? " | Com Seguro" : ""),
                grupo.getValorCredito(), null,
                null, null, null,
                null, null, null);

        return cotaResponse;
    }

    private TipoVendaResponseDTO toResponse(TipoVenda tv) {
        return new TipoVendaResponseDTO(
                tv.getId(), tv.getNome(), tv.getDescricao(), tv.getCanal(),
                tv.getPercentualComissao(), tv.getExigeSeguro(), tv.getPermiteReajuste(),
                tv.getAtivo(), tv.getDataCriacao());
    }

    private String normalizar(String str) {
        if (str == null) return "";
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toUpperCase();
    }
}
