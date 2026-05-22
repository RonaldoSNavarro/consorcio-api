package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.mapper.CotaMapper;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CotaService {

    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;
    private final GrupoRepository grupoRepository;
    private final CotaMapper mapper;

    public CotaService(CotaRepository cotaRepository, ClienteRepository clienteRepository, GrupoRepository grupoRepository, CotaMapper mapper) {
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.grupoRepository = grupoRepository;
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