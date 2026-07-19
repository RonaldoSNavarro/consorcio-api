package br.com.estudo.consorcio.domain.dto;

import br.com.estudo.consorcio.domain.model.Permissao;
import java.util.Set;

public record PerfilResponseDTO(
    Long id,
    String nome,
    Set<Permissao> permissoes
) {}
