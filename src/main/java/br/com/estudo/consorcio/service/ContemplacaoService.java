package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContemplacaoService {

    private final ContemplacaoRepository contemplacaoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final CotaRepository cotaRepository;

    public ContemplacaoService(ContemplacaoRepository contemplacaoRepository, AssembleiaRepository assembleiaRepository, CotaRepository cotaRepository) {
        this.contemplacaoRepository = contemplacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
    }

    @Transactional
    public Contemplacao registrar(Contemplacao contemplacao) {
        // 1. Busca a Assembleia
        Assembleia assembleia = assembleiaRepository.findById(contemplacao.getAssembleia().getId())
                .orElseThrow(() -> new RuntimeException("Assembleia não encontrada."));

        // 2. Busca a Cota
        Cota cota = cotaRepository.findById(contemplacao.getCota().getId())
                .orElseThrow(() -> new RuntimeException("Cota não encontrada."));

        // 3. Regra de Negócio: Cota deve ser do mesmo grupo da Assembleia
        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RuntimeException("A cota e a assembleia pertencem a grupos diferentes.");
        }

        // 4. Regra de Negócio: Apenas cotas ATIVAS podem ser contempladas
        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RuntimeException("Apenas cotas com status ATIVA podem ser contempladas. Status atual: " + cota.getStatus());
        }

        // 5. Salva a Contemplação
        contemplacao.setAssembleia(assembleia);
        contemplacao.setCota(cota);
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        // 6. Atualiza o status da Cota para CONTEMPLADA
        cota.setStatus(StatusCota.CONTEMPLADA);
        cotaRepository.save(cota);

        return contemplacaoSalva;
    }

    public List<Contemplacao> listarPorAssembleia(Long assembleiaId) {
        return contemplacaoRepository.findByAssembleiaId(assembleiaId);
    }
}