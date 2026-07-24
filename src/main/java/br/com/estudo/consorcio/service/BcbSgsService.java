package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.IndiceEconomicoDTO;
import br.com.estudo.consorcio.domain.dto.SimulacaoReajusteResponseDTO;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import br.com.estudo.consorcio.domain.model.IndiceEconomico;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.domain.repository.IndiceEconomicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BcbSgsService {

    private static final Logger logger = LoggerFactory.getLogger(BcbSgsService.class);
    private static final String BCB_SGS_URL_TEMPLATE = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.{serie}/dados/ultimos/12?formato=json";

    private final IndiceEconomicoRepository repository;
    private final RestTemplate restTemplate;

    public BcbSgsService(IndiceEconomicoRepository repository) {
        this.repository = repository;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String obterCodigoSerieBcb(IndiceReajuste tipoIndice) {
        if (tipoIndice == null) return null;
        return switch (tipoIndice) {
            case INCC -> "192";
            case IPCA -> "433";
            case IGP_M -> "189";
            default -> null;
        };
    }

    @Transactional
    public List<IndiceEconomicoDTO> buscarAtualizarUltimos12Meses(IndiceReajuste tipoIndice) {
        String codigoSerie = obterCodigoSerieBcb(tipoIndice);
        if (codigoSerie != null) {
            try {
                String url = BCB_SGS_URL_TEMPLATE.replace("{serie}", codigoSerie);
                ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    for (Map<String, String> item : response.getBody()) {
                        String strData = item.get("data");
                        String strValor = item.get("valor");
                        if (strData != null && strValor != null) {
                            LocalDate dataRef = LocalDate.parse(strData, formatter);
                            BigDecimal valor = new BigDecimal(strValor);

                            Optional<IndiceEconomico> existente = repository.findByTipoIndiceAndDataReferencia(tipoIndice, dataRef);
                            if (existente.isEmpty()) {
                                repository.save(IndiceEconomico.builder()
                                        .tipoIndice(tipoIndice)
                                        .dataReferencia(dataRef)
                                        .valorPercentual(valor)
                                        .dataCaptura(LocalDateTime.now())
                                        .build());
                            } else {
                                IndiceEconomico ind = existente.get();
                                ind.setValorPercentual(valor);
                                ind.setDataCaptura(LocalDateTime.now());
                                repository.save(ind);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Falha ao consultar API do Banco Central para o índice {}: {}. Utilizando cache do banco.", tipoIndice, e.getMessage());
            }
        }

        List<IndiceEconomico> listaBanco = repository.findTop12ByTipoIndiceOrderByDataReferenciaDesc(tipoIndice);
        return listaBanco.stream()
                .map(i -> new IndiceEconomicoDTO(i.getTipoIndice(), i.getDataReferencia(), i.getValorPercentual()))
                .sorted(Comparator.comparing(IndiceEconomicoDTO::dataReferencia))
                .toList();
    }

    public BigDecimal calcularAcumulado12Meses(List<IndiceEconomicoDTO> historico12m) {
        if (historico12m == null || historico12m.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Fator acumulado = PROD(1 + taxa/100) - 1
        BigDecimal fatorAcumulado = BigDecimal.ONE;
        for (IndiceEconomicoDTO dto : historico12m) {
            BigDecimal taxaDecimal = dto.valorPercentual().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            fatorAcumulado = fatorAcumulado.multiply(BigDecimal.ONE.add(taxaDecimal));
        }

        BigDecimal percentualAcumulado = fatorAcumulado.subtract(BigDecimal.ONE).multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
        return percentualAcumulado;
    }

    @Transactional
    public SimulacaoReajusteResponseDTO simularReajuste(IndiceReajuste tipoIndice, BigDecimal valorAtual) {
        if (valorAtual == null || valorAtual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor para simulação deve ser maior que zero.");
        }

        List<IndiceEconomicoDTO> historico12m = buscarAtualizarUltimos12Meses(tipoIndice);
        BigDecimal percentualAcumulado = calcularAcumulado12Meses(historico12m);

        BigDecimal fatorReajuste = BigDecimal.ONE.add(percentualAcumulado.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        BigDecimal novoValorCalculado = valorAtual.multiply(fatorReajuste).setScale(2, RoundingMode.HALF_UP);

        return new SimulacaoReajusteResponseDTO(
                tipoIndice,
                percentualAcumulado,
                fatorReajuste,
                valorAtual,
                novoValorCalculado,
                historico12m
        );
    }
}
