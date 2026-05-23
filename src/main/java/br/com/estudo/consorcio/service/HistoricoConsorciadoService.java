package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.HistoricoConsorciadoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.HistoricoConsorciadoMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.HistoricoConsorciadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoricoConsorciadoService {

    private final HistoricoConsorciadoRepository repository;
    private final HistoricoConsorciadoMapper mapper;

    public HistoricoConsorciadoService(HistoricoConsorciadoRepository repository, HistoricoConsorciadoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Calcula o valor da categoria conforme regra de negócio:
     *
     * Parcela base = (valor_crédito / prazo) + taxa_adm + fundo_reserva + seguros
     *                 └── fundo_comum ──┘
     *
     * Valor da Categoria = Parcela base × prazo
     */
    public BigDecimal calcularValorCategoria(
            BigDecimal valorFundoComum, BigDecimal valorTaxaAdm,
            BigDecimal valorFundoReserva, BigDecimal valorSeguro, int prazoMeses) {

        if (valorFundoComum == null || valorTaxaAdm == null || valorFundoReserva == null || valorSeguro == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal parcelaBase = valorFundoComum
                .add(valorTaxaAdm)
                .add(valorFundoReserva)
                .add(valorSeguro);

        return parcelaBase.multiply(new BigDecimal(prazoMeses));
    }

    @Transactional
    public HistoricoConsorciado registrarInteracao(
            Cliente cliente, Cota cota, Grupo grupo, Parcela parcela,
            TipoInteracao tipo, String descricao,
            BigDecimal valorCredito, BigDecimal valorFundoComum,
            BigDecimal valorTaxaAdm, BigDecimal valorFundoReserva, BigDecimal valorSeguro,
            String descricaoBem, BigDecimal valorBem, Usuario usuario) {

        int prazoMeses = (grupo != null) ? grupo.getPrazoMeses() : 0;
        BigDecimal valorCategoria = calcularValorCategoria(valorFundoComum, valorTaxaAdm, valorFundoReserva, valorSeguro, prazoMeses);

        HistoricoConsorciado historico = HistoricoConsorciado.builder()
                .cliente(cliente)
                .cota(cota)
                .grupo(grupo)
                .parcela(parcela)
                .tipoInteracao(tipo)
                .descricao(descricao)
                .valorCredito(valorCredito)
                .valorFundoComum(valorFundoComum)
                .valorFundoReserva(valorFundoReserva)
                .valorSeguro(valorSeguro)
                .valorCategoria(valorCategoria)
                .descricaoBem(descricaoBem)
                .valorBem(valorBem)
                .numeroParcela(parcela != null ? parcela.getNumeroParcela() : null)
                .valorParcela(parcela != null ? parcela.getValorParcela() : null)
                .dataInteracao(LocalDateTime.now())
                .usuario(usuario)
                .build();

        return repository.save(historico);
    }

    @Transactional(readOnly = true)
    public List<HistoricoConsorciadoResponseDTO> listarPorCliente(Long clienteId) {
        return repository.findByClienteIdOrderByDataInteracaoDesc(clienteId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoConsorciadoResponseDTO> listarPorClienteETipo(Long clienteId, TipoInteracao tipo) {
        return repository.findByClienteIdAndTipoInteracaoOrderByDataInteracaoDesc(clienteId, tipo).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoConsorciadoResponseDTO> listarPorCota(Long cotaId) {
        return repository.findByCotaIdOrderByDataInteracaoDesc(cotaId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
