package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.UsuarioRequestDTO;
import br.com.estudo.consorcio.domain.dto.UsuarioResponseDTO;
import br.com.estudo.consorcio.domain.dto.PerfilResponseDTO;
import br.com.estudo.consorcio.domain.model.Perfil;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.PerfilRepository;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));
        return toDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO salvar(UsuarioRequestDTO dto) {
        Perfil perfil = perfilRepository.findById(dto.perfilId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com ID: " + dto.perfilId()));

        Usuario usuario = new Usuario();
        usuario.setLogin(dto.username());
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setPerfil(perfil);
        
        if (dto.senha() != null && !dto.senha().trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));
        
        Perfil perfil = perfilRepository.findById(dto.perfilId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com ID: " + dto.perfilId()));

        usuario.setLogin(dto.username());
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setPerfil(perfil);

        if (dto.senha() != null && !dto.senha().trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));
        usuarioRepository.delete(usuario);
    }

    private UsuarioResponseDTO toDTO(Usuario usuario) {
        PerfilResponseDTO perfilDTO = null;
        if (usuario.getPerfil() != null) {
            perfilDTO = new PerfilResponseDTO(usuario.getPerfil().getId(), usuario.getPerfil().getNome(), usuario.getPerfil().getPermissoes());
        }
        return new UsuarioResponseDTO(usuario.getId(), usuario.getUsername(), usuario.getNome(), usuario.getEmail(), perfilDTO);
    }
}
