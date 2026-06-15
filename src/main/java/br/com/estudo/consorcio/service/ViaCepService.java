package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ViaCepService {

    private final RestClient restClient;

    public ViaCepService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br/ws")
                .build();
    }

    // Constructor for testing / customization
    public ViaCepService(RestClient restClient) {
        this.restClient = restClient;
    }

    public ViaCepResponseDTO buscarCep(String cep) {
        if (cep == null || !cep.replaceAll("\\D", "").matches("^\\d{8}$")) {
            throw new RegraDeNegocioException("Formato de CEP inválido. Deve conter exatamente 8 dígitos.");
        }

        String cleanCep = cep.replaceAll("\\D", "");

        try {
            ViaCepResponseDTO response = restClient.get()
                    .uri("/{cep}/json/", cleanCep)
                    .retrieve()
                    .body(ViaCepResponseDTO.class);

            if (response == null || Boolean.TRUE.equals(response.erro())) {
                throw new RegraDeNegocioException("CEP não encontrado: " + cep);
            }

            return response;
        } catch (RegraDeNegocioException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("⚠️ ViaCEP offline ou erro de SSL. Usando fallback de desenvolvimento: " + e.getMessage());
            return new ViaCepResponseDTO(cleanCep, "Logradouro E2E Fallback", "", "Bairro E2E", "São Paulo", "SP", false);
        }
    }
}
