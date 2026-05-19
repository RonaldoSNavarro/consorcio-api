package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
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

    public GrupoService(GrupoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GrupoResponseDTO salvar(GrupoRequestDTO dto) {
        // 1. Mapeamento: DTO de entrada para Entidade
        Grupo grupo = new Grupo();
        grupo.setCodigo(dto.codigo());
        grupo.setValorCredito(dto.valorCredito());
        grupo.setPrazoMeses(dto.prazoMeses());
        grupo.setTaxaAdministracao(dto.taxaAdministracao());

        // Regra BCB: Todo grupo nasce em formação (Garantido pelo Back-end)
        grupo.setStatus(StatusGrupo.EM_FORMACAO);

        // 2. Persistência
        Grupo grupoSalvo = repository.save(grupo);

        // 3. Retorno mapeado para DTO de saída
        return converterParaResponseDTO(grupoSalvo);
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

        return converterParaResponseDTO(grupoInaugurado);
    }

    public List<GrupoResponseDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // Método auxiliar para centralizar a conversão de saída
    private GrupoResponseDTO converterParaResponseDTO(Grupo grupo) {
        return new GrupoResponseDTO(
                grupo.getId(),
                grupo.getCodigo(),
                grupo.getValorCredito(),
                grupo.getPrazoMeses(),
                grupo.getTaxaAdministracao(),
                grupo.getStatus(),
                grupo.getDataCriacao(),
                grupo.getDataInauguracao()
        );
    }
}