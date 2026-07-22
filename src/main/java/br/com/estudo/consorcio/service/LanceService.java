package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.LanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.mapper.LanceMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LanceService {

    private final LanceRepository lanceRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final LanceMapper mapper;

    public LanceService(LanceRepository lanceRepository, AssembleiaRepository assembleiaRepository,
                        CotaRepository cotaRepository, ParcelaRepository parcelaRepository, LanceMapper mapper) {
        this.lanceRepository = lanceRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
    }

    @Transactional
    public LanceResponseDTO registrarLance(LanceRequestDTO dto) {
        Assembleia assembleia = assembleiaRepository.findById(dto.assembleiaId())
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        if (assembleia.getStatus() != StatusAssembleia.CAPTANDO) {
            throw new RegraDeNegocioException("A captação de lances não está aberta para esta assembleia.");
        }

        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RegraDeNegocioException("A cota não pertence ao mesmo grupo da assembleia.");
        }

        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RegraDeNegocioException("Apenas cotas ATIVAS podem dar lance.");
        }

        // Validação de inadimplência (regra BACEN)
        boolean inadimplente = parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(
                cota.getId(), StatusParcela.PENDENTE, LocalDate.now());
        if (inadimplente) {
            throw new RegraDeNegocioException("Cota possui parcelas em atraso e não pode participar da assembleia.");
        }

        if (dto.tipo() == TipoLance.FGTS && cota.getGrupo().getCategoriaBem() != br.com.estudo.consorcio.domain.enums.CategoriaBem.IMOVEL) {
            throw new RegraDeNegocioException("O lance utilizando FGTS é permitido apenas para grupos da categoria IMÓVEL.");
        }

        // Determina a modalidade (padrão LIVRE se nulo)
        ModalidadeLance modalidade = dto.modalidade() != null ? dto.modalidade() : ModalidadeLance.LIVRE;
        BigDecimal valorOferta;

        if (modalidade == ModalidadeLance.FIXO) {
            BigDecimal percentual = assembleia.getGrupo().getPercentualLanceFixo();
            if (percentual == null) {
                percentual = new BigDecimal("0.2000");
            }
            valorOferta = assembleia.getGrupo().getValorCredito().multiply(percentual).setScale(2, RoundingMode.HALF_UP);
        } else {
            if (dto.valorOferta() == null || dto.valorOferta().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraDeNegocioException("Valor da oferta deve ser informado e maior que zero para lances livres.");
            }
            valorOferta = dto.valorOferta();
        }

        // Validação de limite para Lance Embutido
        if (dto.tipo() == TipoLance.EMBUTIDO) {
            BigDecimal limitePercentual = assembleia.getGrupo().getPercentualLanceEmbutidoMaximo();
            if (limitePercentual == null) {
                limitePercentual = new BigDecimal("0.30"); // 30% padrão BACEN se não especificado no grupo
            }
            BigDecimal limiteEmbutido = assembleia.getGrupo().getValorCredito().multiply(limitePercentual).setScale(2, RoundingMode.HALF_UP);
            
            if (valorOferta.compareTo(limiteEmbutido) > 0) {
                BigDecimal percentualFormatado = limitePercentual.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
                throw new RegraDeNegocioException("O valor do lance embutido ultrapassa o teto do grupo (" + percentualFormatado + "% - Max R$ " + limiteEmbutido + ").");
            }
        }

        // Lance único por cota (se já existir, atualiza a oferta)
        Optional<Lance> lanceExistente = lanceRepository.findByAssembleiaIdOrderByValorOfertaDesc(assembleia.getId()).stream()
                .filter(l -> l.getCota().getId().equals(cota.getId()))
                .findFirst();

        Lance lance;
        if (lanceExistente.isPresent()) {
            lance = lanceExistente.get();
            lance.setTipo(dto.tipo());
            lance.setModalidade(modalidade);
            lance.setValorOferta(valorOferta);
            lance.setDataOferta(LocalDateTime.now());
        } else {
            lance = new Lance();
            lance.setCota(cota);
            lance.setAssembleia(assembleia);
            lance.setTipo(dto.tipo());
            lance.setModalidade(modalidade);
            lance.setValorOferta(valorOferta);
            // dataOferta e status são definidos no PrePersist
        }

        return mapper.toResponse(lanceRepository.save(lance));
    }

    @Transactional
    public LanceResponseDTO registrarSinistroObito(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() == StatusCota.CONTEMPLADA || cota.getStatus() == StatusCota.QUITADA) {
            throw new RegraDeNegocioException("Cota já contemplada ou quitada não pode gerar lance por sinistro.");
        }

        // Acha a próxima assembleia agendada para o grupo
        Assembleia proximaAssembleia = assembleiaRepository.findByGrupoIdOrderByDataAssembleiaAsc(cota.getGrupo().getId()).stream()
                .filter(a -> a.getStatus() == StatusAssembleia.AGENDADA || a.getStatus() == StatusAssembleia.CAPTANDO)
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Nenhuma assembleia futura encontrada para este grupo."));

        // O valor da oferta será o saldo devedor (valor da carta - fundo comum já pago)
        // Simplificação: o valor do lance é o saldo devedor. Como ele quita o plano, o percentual é calculado
        // pelo motor na AGO e garantirá prioridade máxima no desempate se for 100% de quitação.
        
        // Calcula quanto já foi pago de Fundo Comum
        BigDecimal percentualAmortizado = parcelaRepository.findByCotaId(cotaId).stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .map(p -> p.getPercentualFundoComum() != null ? p.getPercentualFundoComum() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoDevedorPercentual = BigDecimal.ONE.subtract(percentualAmortizado);
        if (saldoDevedorPercentual.compareTo(BigDecimal.ZERO) < 0) saldoDevedorPercentual = BigDecimal.ZERO;
        
        BigDecimal valorOferta = cota.getGrupo().getValorCredito().multiply(saldoDevedorPercentual).setScale(2, RoundingMode.HALF_UP);

        Lance lance = new Lance();
        lance.setCota(cota);
        lance.setAssembleia(proximaAssembleia);
        lance.setTipo(TipoLance.SEGURO_OBITO);
        lance.setModalidade(ModalidadeLance.LIVRE); // Concorre como livre em percentual (100% quitado)
        lance.setValorOferta(valorOferta);

        return mapper.toResponse(lanceRepository.save(lance));
    }
}
