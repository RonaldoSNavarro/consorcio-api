package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrupoService {

    private final GrupoRepository repository;

    public GrupoService(GrupoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Grupo salvar(Grupo grupo) {
        // Regra de negócio: Impedir criação de grupos com o mesmo código (ex: GRP-2026-A)
        if (repository.findByCodigo(grupo.getCodigo()).isPresent()) {
            throw new RuntimeException("Já existe um grupo cadastrado com o código: " + grupo.getCodigo());
        }

        return repository.save(grupo);
    }

    public List<Grupo> listarTodos() {
        return repository.findAll();
    }
}