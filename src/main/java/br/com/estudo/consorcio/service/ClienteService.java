package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    // O Spring injeta o repositório aqui automaticamente
    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {
        // Valida se já existe CPF
        if (repository.findByCpfCnpj(cliente.getCpfCnpj()).isPresent()) {
            throw new RuntimeException("Já existe um cliente cadastrado com este CPF/CNPJ.");
        }

        // Valida se já existe E-mail
        if (repository.findByEmail(cliente.getEmail()).isPresent()) {
            throw new RuntimeException("Já existe um cliente cadastrado com este e-mail.");
        }

        return repository.save(cliente);
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }
}