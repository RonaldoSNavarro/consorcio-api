package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssembleiaService {

    private final AssembleiaRepository assembleiaRepository;
    private final GrupoRepository grupoRepository;

    public AssembleiaService(AssembleiaRepository assembleiaRepository, GrupoRepository grupoRepository) {
        this.assembleiaRepository = assembleiaRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public Assembleia salvar(Assembleia assembleia) {
        // Valida se o grupo foi informado e se existe no banco
        if (assembleia.getGrupo() == null || assembleia.getGrupo().getId() == null) {
            throw new RuntimeException("O grupo é obrigatório para agendar uma assembleia.");
        }

        if (!grupoRepository.existsById(assembleia.getGrupo().getId())) {
            throw new RuntimeException("Grupo não encontrado no banco de dados.");
        }

        return assembleiaRepository.save(assembleia);
    }

    public List<Assembleia> listarPorGrupo(Long grupoId) {
        return assembleiaRepository.findByGrupoId(grupoId);
    }
}