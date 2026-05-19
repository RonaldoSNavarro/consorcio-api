package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ClienteResponseDTO salvar(ClienteRequestDTO dto) {
        // 1. Validações de Negócio (Usando os dados do DTO)
        if (repository.findByCpfCnpj(dto.cpfCnpj()).isPresent()) {
            throw new RegraDeNegocioException("Já existe um cliente cadastrado com este CPF/CNPJ.");
        }

        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new RegraDeNegocioException("Já existe um cliente cadastrado com este e-mail.");
        }

        // 2. Mapeamento Manual: DTO -> Entidade
        Cliente cliente = new Cliente();
        cliente.setNome(dto.nome());
        cliente.setCpfCnpj(dto.cpfCnpj());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());

        // 3. Persistência
        Cliente clienteSalvo = repository.save(cliente);

        // 4. Mapeamento de Saída: Entidade -> DTO
        return converterParaResponseDTO(clienteSalvo);
    }

    public List<ClienteResponseDTO> listarTodos() {
        // Transforma a lista de entidades em uma lista de DTOs de resposta
        return repository.findAll()
                .stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // Método auxiliar privado para evitar repetição de código
    private ClienteResponseDTO converterParaResponseDTO(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getDataCadastro()
        );
    }
}