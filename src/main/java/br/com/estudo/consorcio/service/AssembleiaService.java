package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.domain.mapper.AssembleiaMapper; // Importar o mapper
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssembleiaService {

    private final AssembleiaRepository assembleiaRepository;
    private final GrupoRepository grupoRepository;
    private final AssembleiaMapper mapper; // Injetar o mapper

    public AssembleiaService(AssembleiaRepository assembleiaRepository, GrupoRepository grupoRepository, AssembleiaMapper mapper) { // Adicionar o mapper ao construtor
        this.assembleiaRepository = assembleiaRepository;
        this.grupoRepository = grupoRepository;
        this.mapper = mapper;
    }

    @Transactional
    public AssembleiaResponseDTO salvar(AssembleiaRequestDTO dto) {
        // 1. Busca o grupo no banco
        Grupo grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado para agendar assembleia."));

        // 2. Mapeamento: DTO -> Entidade usando o mapper
        Assembleia assembleia = mapper.toEntity(dto);
        assembleia.setGrupo(grupo); // Setar o grupo após a busca

        // Regra de negócio: se não informar o tipo, assume ORDINARIA
        assembleia.setTipo(dto.tipo() != null ? dto.tipo() : TipoAssembleia.ORDINARIA);

        // 3. Persistência
        Assembleia assembleiaSalva = assembleiaRepository.save(assembleia);

        // 4. Retorno mapeado para ResponseDTO usando o mapper
        return mapper.toResponse(assembleiaSalva);
    }

    public List<AssembleiaResponseDTO> listarPorGrupo(Long grupoId) {
        return assembleiaRepository.findByGrupoId(grupoId).stream()
                .map(mapper::toResponse) // Usar o mapper
                .toList();
    }

    // O método auxiliar converterParaResponseDTO foi removido, pois o mapper faz esse trabalho.
}