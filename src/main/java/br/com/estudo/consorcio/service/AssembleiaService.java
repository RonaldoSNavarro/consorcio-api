package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.TipoAssembleia;
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
    public AssembleiaResponseDTO salvar(AssembleiaRequestDTO dto) {
        // 1. Busca o grupo no banco
        Grupo grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado para agendar assembleia."));

        // 2. Mapeamento: DTO -> Entidade
        Assembleia assembleia = new Assembleia();
        assembleia.setDataAssembleia(dto.dataAssembleia());
        assembleia.setGrupo(grupo);

        // Regra de negócio: se não informar o tipo, assume ORDINARIA
        assembleia.setTipo(dto.tipo() != null ? dto.tipo() : TipoAssembleia.ORDINARIA);

        // 3. Persistência
        Assembleia assembleiaSalva = assembleiaRepository.save(assembleia);

        // 4. Retorno mapeado para ResponseDTO
        return converterParaResponseDTO(assembleiaSalva);
    }

    public List<AssembleiaResponseDTO> listarPorGrupo(Long grupoId) {
        return assembleiaRepository.findByGrupoId(grupoId).stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    private AssembleiaResponseDTO converterParaResponseDTO(Assembleia assembleia) {
        return new AssembleiaResponseDTO(
                assembleia.getId(),
                assembleia.getDataAssembleia(),
                assembleia.getTipo(),
                assembleia.getGrupo().getId()
        );
    }
}