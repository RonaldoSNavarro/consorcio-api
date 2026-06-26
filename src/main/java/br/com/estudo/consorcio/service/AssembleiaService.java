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
    private final AssembleiaMapper mapper;
    private final MotorApuracaoService motorApuracaoService;

    public AssembleiaService(AssembleiaRepository assembleiaRepository, GrupoRepository grupoRepository, 
                             AssembleiaMapper mapper, MotorApuracaoService motorApuracaoService) {
        this.assembleiaRepository = assembleiaRepository;
        this.grupoRepository = grupoRepository;
        this.mapper = mapper;
        this.motorApuracaoService = motorApuracaoService;
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

    @Transactional
    public void abrirCaptacao(Long assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        if (assembleia.getStatus() != br.com.estudo.consorcio.domain.model.StatusAssembleia.AGENDADA) {
            throw new RegraDeNegocioException("Apenas assembleias AGENDADAS podem abrir captação de lances.");
        }

        assembleia.setStatus(br.com.estudo.consorcio.domain.model.StatusAssembleia.CAPTANDO);
        assembleia.setDataInicioCaptacao(java.time.LocalDateTime.now());
        assembleiaRepository.save(assembleia);
    }

    @Transactional
    public void fecharCaptacao(Long assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        if (assembleia.getStatus() != br.com.estudo.consorcio.domain.model.StatusAssembleia.CAPTANDO) {
            throw new RegraDeNegocioException("Apenas assembleias com status CAPTANDO podem ser fechadas.");
        }

        assembleia.setStatus(br.com.estudo.consorcio.domain.model.StatusAssembleia.REALIZADA);
        assembleia.setDataFimCaptacao(java.time.LocalDateTime.now());
        assembleiaRepository.save(assembleia);
    }

    @Transactional
    public void apurarAssembleia(Long assembleiaId) {
        motorApuracaoService.apurarAssembleia(assembleiaId, null);
    }

    @Transactional
    public void apurarAssembleia(Long assembleiaId, br.com.estudo.consorcio.domain.dto.ApuracaoRequestDTO params) {
        motorApuracaoService.apurarAssembleia(assembleiaId, params);
    }
}