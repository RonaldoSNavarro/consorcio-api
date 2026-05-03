package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // O Spring Data cria o SQL automaticamente (SELECT * FROM clientes WHERE cpf_cnpj = ?)
    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    Optional<Cliente> findByEmail(String email);
}