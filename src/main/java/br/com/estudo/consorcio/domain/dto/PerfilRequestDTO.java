package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.Permissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record PerfilRequestDTO(
    @NotBlank(message = "O nome do perfil é obrigatório")
    String nome,

    @NotEmpty(message = "O perfil deve ter ao menos uma permissão")
    Set<Permissao> permissoes
) {}
