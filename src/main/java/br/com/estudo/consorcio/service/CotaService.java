package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaReembolsoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.mapper.CotaMapper;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.repository.HistoricoVersaoCotaRepository;
import br.com.estudo.consorcio.domain.dto.HistoricoVersaoCotaResponseDTO;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
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

    public CotaService(CotaRepository cotaRepository, ClienteRepository clienteRepository,
                       GrupoRepository grupoRepository, ParcelaRepository parcelaRepository,
                       CotaMapper mapper, MovimentoFinanceiroService movimentoService,
                       HistoricoVersaoCotaRepository historicoVersaoCotaRepository,
                       HistoricoConsorciadoService historicoService) {
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.grupoRepository = grupoRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.historicoVersaoCotaRepository = historicoVersaoCotaRepository;
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

        Grupo grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        // 2. Mapeamento para a Entidade usando o mapper
        Cota cota = mapper.toEntity(dto);
        cota.setCliente(cliente);
        cota.setGrupo(grupo);

        // Garante a regra de negócio da cota nascer ATIVA (reforçando o @PrePersist)
        cota.setStatus(StatusCota.ATIVA);

        // 3. Persistência
        Cota cotaSalva = cotaRepository.save(cota);

        // 4. Retorno mapeado usando o mapper
        return mapper.toResponse(cotaSalva);
    }

    @Transactional
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

        return mapper.toResponse(cota);
    }

    @Transactional
    public CotaReembolsoResponseDTO reembolsarCota(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() != StatusCota.CANCELADA) {
            throw new RegraDeNegocioException("Apenas cotas canceladas/excluídas podem ser reembolsadas.");
        }

        if (Boolean.TRUE.equals(cota.getReembolsada())) {
            throw new RegraDeNegocioException("Esta cota já foi reembolsada.");
        }

        // Soma o total pago ao Fundo Comum
        List<Parcela> parcelasPagas = parcelaRepository.findByCotaId(cotaId).stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .toList();

        BigDecimal totalFundoComumPago = parcelasPagas.stream()
                .map(Parcela::getValorFundoComum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Multa rescisória de 10% de cláusula penal pela rescisão do contrato de consórcio
        BigDecimal multaRescisoria = totalFundoComumPago.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorReembolsado = totalFundoComumPago.subtract(multaRescisoria).setScale(2, RoundingMode.HALF_UP);

        // Atualiza a cota
        cota.setValorReembolsado(valorReembolsado);
        cota.setReembolsada(true);
        cotaRepository.save(cota);

        // --- Registrar Movimentos Financeiros (Módulo 2) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = cota.getGrupo();

        if (valorReembolsado.compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, null, null,
                    TipoMovimentoFinanceiro.REEMBOLSO, NaturezaMovimento.DEBITO,
                    valorReembolsado, "Reembolso de cota cancelada - Cota " + cota.getNumeroCota(), usuario);
        }

        if (multaRescisoria.compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, null, null,
                    TipoMovimentoFinanceiro.MULTA_RESCISORIA, NaturezaMovimento.CREDITO,
                    multaRescisoria, "Multa rescisória de cota cancelada - Cota " + cota.getNumeroCota(), usuario);
        }

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.REEMBOLSO, "Reembolso efetuado para a cota cancelada. Valor pago FC: R$ " + totalFundoComumPago + " | Multa: R$ " + multaRescisoria + " | Reembolsado: R$ " + valorReembolsado,
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

    public Page<CotaResponseDTO> listarTodas(Pageable pageable) {
        return cotaRepository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public Page<CotaResponseDTO> listarPorCliente(Long clienteId, Pageable pageable) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado."));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            if (!username.equals("admin") && !username.equals(cliente.getCpfCnpj()) && !username.equals(cliente.getEmail())) {
                throw new AccessDeniedException("Acesso negado. Você só pode acessar seus próprios dados.");
            }
        }

        return cotaRepository.findByClienteId(clienteId, pageable)
                .map(mapper::toResponse);
    }

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
}