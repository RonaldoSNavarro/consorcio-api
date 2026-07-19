package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.PerfilRequestDTO;
import br.com.estudo.consorcio.domain.dto.PerfilResponseDTO;
import br.com.estudo.consorcio.domain.model.Perfil;
import br.com.estudo.consorcio.domain.repository.PerfilRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;

    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    public List<PerfilResponseDTO> listarTodos() {
        return perfilRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PerfilResponseDTO buscarPorId(Long id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com ID: " + id));
        return toDTO(perfil);
    }

    @Transactional
    public PerfilResponseDTO salvar(PerfilRequestDTO dto) {
        Perfil perfil = new Perfil();
        perfil.setNome(dto.nome().toUpperCase());
        perfil.setPermissoes(dto.permissoes());
        return toDTO(perfilRepository.save(perfil));
    }

    @Transactional
    public PerfilResponseDTO atualizar(Long id, PerfilRequestDTO dto) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com ID: " + id));
        
        perfil.setNome(dto.nome().toUpperCase());
        perfil.setPermissoes(dto.permissoes());
        return toDTO(perfilRepository.save(perfil));
    }

    @Transactional
    public void deletar(Long id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com ID: " + id));
        perfilRepository.delete(perfil);
    }

    private PerfilResponseDTO toDTO(Perfil perfil) {
        return new PerfilResponseDTO(perfil.getId(), perfil.getNome(), perfil.getPermissoes());
    }
}
