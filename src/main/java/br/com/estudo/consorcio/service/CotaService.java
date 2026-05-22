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
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CotaService {

    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;
    private final GrupoRepository grupoRepository;
    private final ParcelaRepository parcelaRepository;
    private final CotaMapper mapper;

    public CotaService(CotaRepository cotaRepository, ClienteRepository clienteRepository, GrupoRepository grupoRepository, ParcelaRepository parcelaRepository, CotaMapper mapper) {
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.grupoRepository = grupoRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
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

        // Altera status para CANCELADA
        cota.setStatus(StatusCota.CANCELADA);
        Cota cotaSalva = cotaRepository.save(cota);

        // Remove todas as parcelas PENDENTES (ou atrasadas/abertas) da cota
        List<Parcela> parcelasPendentes = parcelaRepository.findByCotaId(cotaId).stream()
                .filter(p -> p.getStatus() == StatusParcela.PENDENTE)
                .toList();

        parcelaRepository.deleteAll(parcelasPendentes);

        return mapper.toResponse(cotaSalva);
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

        return new CotaReembolsoResponseDTO(
                cotaId,
                cota.getNumeroCota(),
                totalFundoComumPago,
                multaRescisoria,
                valorReembolsado,
                true
        );
    }

    public List<CotaResponseDTO> listarTodas() {
        return cotaRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<CotaResponseDTO> listarPorCliente(Long clienteId) {
        return cotaRepository.findByClienteId(clienteId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<CotaResponseDTO> listarPorGrupo(Long grupoId) {
        return cotaRepository.findByGrupoId(grupoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private void validarClienteAtivo(Cliente cliente) {
        if (StatusCliente.INATIVO.equals(cliente.getStatus())) {
            throw new ClienteInativoException(
                    "Operação não permitida: cliente id " + cliente.getId() + " está inativo.");
        }
    }
}