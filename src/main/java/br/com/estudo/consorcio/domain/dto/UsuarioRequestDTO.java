package br.com.estudo.consorcio.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

public record UsuarioRequestDTO(
    @NotBlank(message = "O username é obrigatório")
    String username,
    
    String senha,
    
    @NotNull(message = "O id do perfil é obrigatório")
    Long perfilId,
    
    @NotBlank(message = "O nome é obrigatório")
    String nome,
    
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email
) {}
