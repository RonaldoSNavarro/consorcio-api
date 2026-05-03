package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CotaService {

    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;
    private final GrupoRepository grupoRepository;

    // O Spring injeta automaticamente todos os repositórios necessários
    public CotaService(CotaRepository cotaRepository, ClienteRepository clienteRepository, GrupoRepository grupoRepository) {
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public Cota salvar(Cota cota) {
        // 1. Valida se o Cliente foi informado e se existe no banco
        if (cota.getCliente() == null || cota.getCliente().getId() == null || !clienteRepository.existsById(cota.getCliente().getId())) {
            throw new RuntimeException("Cliente inválido ou não encontrado.");
        }

        // 2. Valida se o Grupo foi informado e se existe no banco
        if (cota.getGrupo() == null || cota.getGrupo().getId() == null || !grupoRepository.existsById(cota.getGrupo().getId())) {
            throw new RuntimeException("Grupo inválido ou não encontrado.");
        }

        // Se passar pelas validações, salva a cota
        return cotaRepository.save(cota);
    }

    public List<Cota> listarTodas() {
        return cotaRepository.findAll();
    }

    public List<Cota> listarPorCliente(Long clienteId) {
        return cotaRepository.findByClienteId(clienteId);
    }

    public List<Cota> listarPorGrupo(Long grupoId) {
        return cotaRepository.findByGrupoId(grupoId);
    }
}