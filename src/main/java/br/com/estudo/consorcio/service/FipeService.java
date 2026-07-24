package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.FipeItemDTO;
import br.com.estudo.consorcio.domain.dto.FipeModelosResponseDTO;
import br.com.estudo.consorcio.domain.dto.FipeValorDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class FipeService {

    private static final String FIPE_BASE_URL = "https://parallelum.com.br/fipe/api/v1/carros";
    private final RestTemplate restTemplate;

    public FipeService() {
        this.restTemplate = new RestTemplate();
    }

    public List<FipeItemDTO> listarMarcas() {
        try {
            ResponseEntity<List<FipeItemDTO>> response = restTemplate.exchange(
                    FIPE_BASE_URL + "/marcas",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<FipeItemDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public FipeModelosResponseDTO listarModelos(String marcaId) {
        try {
            return restTemplate.getForObject(FIPE_BASE_URL + "/marcas/" + marcaId + "/modelos", FipeModelosResponseDTO.class);
        } catch (Exception e) {
            return new FipeModelosResponseDTO(Collections.emptyList(), Collections.emptyList());
        }
    }

    public List<FipeItemDTO> listarAnos(String marcaId, String modeloId) {
        try {
            ResponseEntity<List<FipeItemDTO>> response = restTemplate.exchange(
                    FIPE_BASE_URL + "/marcas/" + marcaId + "/modelos/" + modeloId + "/anos",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<FipeItemDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public FipeValorDTO consultarValor(String marcaId, String modeloId, String anoId) {
        try {
            String url = String.format("%s/marcas/%s/modelos/%s/anos/%s", FIPE_BASE_URL, marcaId, modeloId, anoId);
            return restTemplate.getForObject(url, FipeValorDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public BigDecimal parseValorFipe(String valorFipeStr) {
        if (valorFipeStr == null || valorFipeStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            // Ex: "R$ 75.490,00" -> 75490.00
            String limpo = valorFipeStr.replace("R$", "").replace(".", "").replace(",", ".").trim();
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
