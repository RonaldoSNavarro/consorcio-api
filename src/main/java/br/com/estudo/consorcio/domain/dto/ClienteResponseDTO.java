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
        Boolean pep,
        LocalDate dataCadastro,
        StatusCliente statusCliente
) {
    public ClienteResponseDTO(Long id, String nome, String cpfCnpj, String email, String telefone,
                              String cep, String logradouro, String numero, String complemento,
                              String bairro, String localidade, String uf, BigDecimal patrimonio,
                              BigDecimal rendaMensal, NivelRisco nivelRisco, LocalDate dataCadastro,
                              StatusCliente statusCliente) {
        this(id, nome, cpfCnpj, email, telefone, cep, logradouro, numero, complemento, bairro, localidade, uf, patrimonio, rendaMensal, nivelRisco, false, dataCadastro, statusCliente);
    }
}