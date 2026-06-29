package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import java.math.BigDecimal;
import java.time.LocalDate;


public record ClienteResponseDTO(
        Long id,
        String nome,
        String cpfCnpj,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        BigDecimal patrimonio,
        BigDecimal rendaMensal,
        NivelRisco nivelRisco,
        LocalDate dataCadastro,
        StatusCliente statusCliente
) {
    public ClienteResponseDTO(Long id, String nome, String cpfCnpj, String email, String telefone,
                              String cep, String logradouro, String numero, String complemento,
                              String bairro, String localidade, String uf, BigDecimal patrimonio,
                              BigDecimal rendaMensal, NivelRisco nivelRisco, LocalDate dataCadastro,
                              StatusCliente statusCliente) {
        this.id = id;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.telefone = telefone;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.localidade = localidade;
        this.uf = uf;
        this.patrimonio = patrimonio;
        this.rendaMensal = rendaMensal;
        this.nivelRisco = nivelRisco;
        this.dataCadastro = dataCadastro;
        this.statusCliente = statusCliente;
    }
}