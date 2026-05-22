package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.GrupoMapper; // Importar o mapper
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GrupoService {

    private final GrupoRepository repository;
    private final GrupoMapper mapper; // Injetar o mapper

    public GrupoService(GrupoRepository repository, GrupoMapper mapper) { // Adicionar o mapper ao construtor
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public GrupoResponseDTO salvar(GrupoRequestDTO dto) {
        // 1. Mapeamento: DTO de entrada para Entidade usando o mapper
        Grupo grupo = mapper.toEntity(dto);

        // Regra BCB: Todo grupo nasce em formação (Garantido pelo Back-end)
        grupo.setStatus(StatusGrupo.EM_FORMACAO);

        // 2. Persistência
        Grupo grupoSalvo = repository.save(grupo);

        // 3. Retorno mapeado para DTO de saída usando o mapper
        return mapper.toResponse(grupoSalvo);
    }

    @Transactional
    public GrupoResponseDTO inaugurar(Long id, LocalDate dataAssembleia) {
        Grupo grupo = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() != StatusGrupo.EM_FORMACAO) {
            throw new RegraDeNegocioException("Apenas grupos em formação podem ser inaugurados.");
        }

        // Regra BCB: O grupo é inaugurado na data da 1ª Assembleia Geral Ordinária (AGO)
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupo.setDataInauguracao(dataAssembleia);

        Grupo grupoInaugurado = repository.save(grupo);

        return mapper.toResponse(grupoInaugurado); // Usar o mapper
    }

    public List<GrupoResponseDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse) // Usar o mapper
                .toList();
    }

    // O método auxiliar converterParaResponseDTO foi removido, pois o mapper faz esse trabalho.
}