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

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Cliente c WHERE :search IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :search, '%')) OR c.cpfCnpj LIKE CONCAT('%', :search, '%')")
    org.springframework.data.domain.Page<Cliente> buscarPorPesquisa(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}