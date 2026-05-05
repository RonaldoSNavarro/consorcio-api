package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
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
    public Grupo salvar(Grupo grupo) {
        // Regra BCB: Todo grupo nasce em formação
        if (grupo.getStatus() == null) {
            grupo.setStatus(StatusGrupo.EM_FORMACAO);
        }
        return repository.save(grupo);
    }

    @Transactional
    public Grupo inaugurar(Long id, LocalDate dataAssembleia) {
        Grupo grupo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado."));

        if (grupo.getStatus() != StatusGrupo.EM_FORMACAO) {
            throw new RuntimeException("Apenas grupos em formação podem ser inaugurados.");
        }

        // Regra BCB: O grupo é inaugurado na data da 1ª Assembleia Geral Ordinária (AGO)
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupo.setDataInauguracao(dataAssembleia);

        return repository.save(grupo);
    }

    public List<Grupo> listarTodos() {
        return repository.findAll();
    }
}