package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import br.com.estudo.consorcio.domain.mapper.ClienteMapper;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.DocumentoJaCadastradoException;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final ClienteMapper mapper;
    private final ViaCepService viaCepService;

    public ClienteService(ClienteRepository repository, ClienteMapper mapper, ViaCepService viaCepService) {
        this.repository = repository;
        this.mapper = mapper;
        this.viaCepService = viaCepService;
    }

    // -------------------------------------------------------------------------
    // CADASTRO
    // -------------------------------------------------------------------------

    @Transactional
    public ClienteResponseDTO salvar(ClienteRequestDTO dto) {
        validarDocumentoUnico(dto.cpfCnpj()); // CORRIGIDO: dto.documento() para dto.cpfCnpj()

        ViaCepResponseDTO viaCep = viaCepService.buscarCep(dto.cep());

        Cliente cliente = mapper.toEntity(dto);
        cliente.setStatus(StatusCliente.ATIVO);
        cliente.setLogradouro(viaCep.logradouro());
        cliente.setBairro(viaCep.bairro());
        cliente.setLocalidade(viaCep.localidade());
        cliente.setUf(viaCep.uf());

        return mapper.toResponse(repository.save(cliente));
    }

    // -------------------------------------------------------------------------
    // LEITURA
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listarTodos(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntidadePorId(id));
    }

    // -------------------------------------------------------------------------
    // ATUALIZAÇÃO
    // -------------------------------------------------------------------------

    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);

        validarClienteAtivo(cliente);

        /*
         * CPF/CNPJ não pode ser alterado após o cadastro.
         * Regra de negócio baseada na Lei do Consórcio (Lei 11.795/2008),
         * que exige identificação inequívoca do consorciado durante todo
         * o ciclo do grupo.
         */
        if (!cliente.getCpfCnpj().equals(dto.cpfCnpj())) { // CORRIGIDO: cliente.getDocumento() para cliente.getCpfCnpj() e dto.documento() para dto.cpfCnpj()
            throw new IllegalArgumentException(
                    "O documento (CPF/CNPJ) não pode ser alterado após o cadastro.");
        }

        ViaCepResponseDTO viaCep = viaCepService.buscarCep(dto.cep());
        mapper.updateEntityFromDto(dto, cliente);
        cliente.setLogradouro(viaCep.logradouro());
        cliente.setBairro(viaCep.bairro());
        cliente.setLocalidade(viaCep.localidade());
        cliente.setUf(viaCep.uf());

        return mapper.toResponse(repository.save(cliente));
    }

    // -------------------------------------------------------------------------
    // INATIVAÇÃO LÓGICA
    // -------------------------------------------------------------------------

    @Transactional
    public void inativar(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        validarClienteAtivo(cliente);

        /*
         * Inativação lógica: o registro é preservado conforme:
         *  - LGPD Art. 16, II  → obrigação legal ou regulatória
         *  - LGPD Art. 16, IV  → exercício regular de direitos (cobranças, auditoria)
         *  - BCB Resolução 285 → administradoras devem manter histórico dos consorciados
         */
        cliente.setStatus(StatusCliente.INATIVO);
        repository.save(cliente);
    }

    // -------------------------------------------------------------------------
    // MÉTODOS AUXILIARES (package-private para reuso em outros services)
    // -------------------------------------------------------------------------

    Cliente buscarEntidadePorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado com id: " + id));
    }

    private void validarDocumentoUnico(String documento) {
        if (repository.findByCpfCnpj(documento).isPresent()) { // CORRIGIDO: existsByDocumento para findByCpfCnpj().isPresent()
            throw new DocumentoJaCadastradoException(
                    "Já existe um cliente cadastrado com o documento: " + documento);
        }
    }

    private void validarClienteAtivo(Cliente cliente) {
        if (StatusCliente.INATIVO.equals(cliente.getStatus())) {
            throw new ClienteInativoException(
                    "Operação não permitida: cliente id " + cliente.getId() + " está inativo.");
        }
    }
}