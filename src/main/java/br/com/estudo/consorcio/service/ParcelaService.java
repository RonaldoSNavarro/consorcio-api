package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ParcelaService {

    private final ParcelaRepository parcelaRepository;
    private final CotaRepository cotaRepository;

    public ParcelaService(ParcelaRepository parcelaRepository, CotaRepository cotaRepository) {
        this.parcelaRepository = parcelaRepository;
        this.cotaRepository = cotaRepository;
    }

    @Transactional
    public Parcela salvar(Parcela parcela) {
        // Valida se a Cota foi informada e existe
        if (parcela.getCota() == null || parcela.getCota().getId() == null || !cotaRepository.existsById(parcela.getCota().getId())) {
            throw new RuntimeException("Cota inválida ou não encontrada no banco de dados.");
        }

        // Garante que toda parcela nova nasça como pendente (caso não venha na requisição)
        if (parcela.getStatus() == null) {
            parcela.setStatus(StatusParcela.PENDENTE); // CORREÇÃO AQUI 2: Usando o Enum
        }

        return parcelaRepository.save(parcela);
    }

    @Transactional
    public Parcela pagar(Long parcelaId) {
        // Busca a parcela no banco
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela não encontrada."));

        // Regra: Não pode pagar uma parcela que já foi paga
        if (parcela.getStatus() == StatusParcela.PAGA) { // CORREÇÃO AQUI 3: Comparando com Enum
            throw new RuntimeException("Esta parcela já consta como paga.");
        }

        // Atualiza os dados de pagamento
        parcela.setStatus(StatusParcela.PAGA); // CORREÇÃO AQUI 4: Usando o Enum
        parcela.setDataPagamento(LocalDate.now());

        return parcelaRepository.save(parcela);
    }

    public List<Parcela> listarPorCota(Long cotaId) {
        return parcelaRepository.findByCotaId(cotaId);
    }
}