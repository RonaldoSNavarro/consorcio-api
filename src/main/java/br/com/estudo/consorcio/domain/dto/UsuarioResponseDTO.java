package br.com.estudo.consorcio.domain.dto;

public record UsuarioResponseDTO(
    Long id,
    String username,
    String nome,
    String email,
    PerfilResponseDTO perfil
) {}
