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

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() > 3 ? p.substring(0, 3) + "***" : p.substring(0, 1) + "**";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0 || i == parts.length - 1) {
                sb.append(part.length() > 2 ? part.substring(0, 2) + "***" : part.substring(0, 1) + "*").append(" ");
            } else {
                sb.append("*** ");
            }
        }
        return sb.toString().trim();
    }

    private static String maskCpfCnpj(String value) {
        if (value == null || value.isBlank()) return value;
        String clean = value.replaceAll("\\D", "");
        if (clean.length() == 11) {
            return "***." + clean.substring(3, 6) + "." + clean.substring(6, 9) + "-**";
        } else if (clean.length() == 14) {
            return clean.substring(0, 3) + ".***.***/***-" + clean.substring(12);
        }
        return "***" + (value.length() > 4 ? value.substring(value.length() - 4) : "");
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int idx = email.indexOf("@");
        String local = email.substring(0, idx);
        String domain = email.substring(idx);
        String maskedLocal = local.length() > 3 ? local.substring(0, 3) + "***" : local.substring(0, 1) + "**";
        return maskedLocal + domain;
    }

    private static String maskTelefone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String clean = phone.replaceAll("\\D", "");
        if (clean.length() == 11) {
            return "(" + clean.substring(0, 2) + ") 9" + clean.substring(3, 7) + "-****";
        } else if (clean.length() == 10) {
            return "(" + clean.substring(0, 2) + ") " + clean.substring(2, 6) + "-****";
        }
        return "***" + (phone.length() > 4 ? phone.substring(phone.length() - 4) : "");
    }
}