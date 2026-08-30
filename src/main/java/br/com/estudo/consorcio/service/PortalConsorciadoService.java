package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.LanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.dto.PortalCotaDTO;
import br.com.estudo.consorcio.domain.dto.PortalExtratoItemDTO;
import br.com.estudo.consorcio.domain.dto.PortalOfertaLanceDTO;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusAssembleia;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.model.TipoLance;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortalConsorciadoService {

    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final ClienteRepository clienteRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final LanceService lanceService;

    public PortalConsorciadoService(CotaRepository cotaRepository,
                                  ParcelaRepository parcelaRepository,
                                  ClienteRepository clienteRepository,
                                  AssembleiaRepository assembleiaRepository,
                                  LanceService lanceService) {
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.clienteRepository = clienteRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.lanceService = lanceService;
    }

    @Transactional(readOnly = true)
    public List<PortalCotaDTO> listarCotasDoCliente(String cpfOuCnpj) {
        Cliente cliente = clienteRepository.findByCpfCnpj(cpfOuCnpj)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consorciado não encontrado para o documento: " + cpfOuCnpj));

        List<Cota> cotas = cotaRepository.findByClienteId(cliente.getId());

        return cotas.stream().map(cota -> {
            List<Parcela> parcelas = parcelaRepository.findByCotaId(cota.getId());
            long pagas = parcelas.stream().filter(p -> p.getStatus() == StatusParcela.PAGA).count();
            BigDecimal saldoDevedor = parcelas.stream()
                    .filter(p -> p.getStatus() == StatusParcela.PENDENTE)
                    .map(Parcela::getValorParcela)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorCredito = cota.getGrupo() != null ? cota.getGrupo().getValorCredito() : BigDecimal.ZERO;
            int total = parcelas.isEmpty() ? (cota.getGrupo() != null ? cota.getGrupo().getPrazoMeses() : 0) : parcelas.size();

            return new PortalCotaDTO(
                    cota.getId(),
                    cota.getGrupo() != null ? cota.getGrupo().getCodigoGrupo() : null,
                    cota.getCodigoCota(),
                    cota.getStatus(),
                    valorCredito,
                    saldoDevedor,
                    (int) pagas,
                    total
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PortalExtratoItemDTO> obterExtratoCota(Long cotaId, String cpfOuCnpj) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cota não encontrada"));

        if (cota.getCliente() == null || !cota.getCliente().getCpfCnpj().equals(cpfOuCnpj)) {
            throw new RegraDeNegocioException("Acesso negado: a cota informada não pertence ao consorciado autenticado");
        }

        List<Parcela> parcelas = parcelaRepository.findByCotaId(cota.getId());
        return parcelas.stream()
                .map(p -> new PortalExtratoItemDTO(
                        p.getId(),
                        p.getNumeroParcela(),
                        p.getDataVencimento(),
                        p.getValorParcela(),
                        p.getValorPago(),
                        p.getStatus(),
                        p.getDataPagamento()
                ))
                .toList();
    }

    @Transactional
    public LanceResponseDTO ofertarLanceOnline(PortalOfertaLanceDTO dto, String cpfOuCnpj) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cota não encontrada"));

        if (cota.getCliente() == null || !cota.getCliente().getCpfCnpj().equals(cpfOuCnpj)) {
            throw new RegraDeNegocioException("Acesso negado: a cota informada não pertence ao consorciado autenticado");
        }

        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RegraDeNegocioException("Apenas cotas ATIVAS e não contempladas podem ofertar lance");
        }

        List<Assembleia> assembleias = assembleiaRepository.findByGrupoId(cota.getGrupo().getId());
        Assembleia proxima = assembleias.stream()
                .filter(a -> a.getStatus() == StatusAssembleia.AGENDADA)
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Não há assembleia aberta para captação de lances no momento"));

        TipoLance tipo = dto.tipoLance() != null ? TipoLance.valueOf(dto.tipoLance()) : TipoLance.FIRME;
        LanceRequestDTO req = new LanceRequestDTO(
                cota.getId(),
                proxima.getId(),
                tipo,
                dto.valorLance(),
                ModalidadeLance.LIVRE
        );

        return lanceService.registrarLance(req);
    }
}