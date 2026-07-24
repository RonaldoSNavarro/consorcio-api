package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
import br.com.estudo.consorcio.domain.model.CategoriaBem;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.domain.repository.CategoriaBemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CategoriaBemDataLoader implements CommandLineRunner {

    private final CategoriaBemRepository repository;

    public CategoriaBemDataLoader(CategoriaBemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        List<CategoriaBem> desejadas = List.of(
            CategoriaBem.builder().nome("Veículos Automotores").tipoBacen(TipoCategoriaBacen.BEM_MOVEL_I).indiceReajustePadrao(IndiceReajuste.FIPE).build(),
            CategoriaBem.builder().nome("Imóveis").tipoBacen(TipoCategoriaBacen.BEM_IMOVEL).indiceReajustePadrao(IndiceReajuste.INCC).build(),
            CategoriaBem.builder().nome("Serviços").tipoBacen(TipoCategoriaBacen.SERVICO).indiceReajustePadrao(IndiceReajuste.IPCA).build(),
            CategoriaBem.builder().nome("Outros Bens Móveis").tipoBacen(TipoCategoriaBacen.BEM_MOVEL_II).indiceReajustePadrao(IndiceReajuste.IPCA).build()
        );

        for (CategoriaBem c : desejadas) {
            if (repository.findByTipoBacen(c.getTipoBacen()).isEmpty()) {
                repository.save(c);
            }
        }
    }
}
