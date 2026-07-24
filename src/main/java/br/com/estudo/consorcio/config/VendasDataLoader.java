package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.enums.TipoVendaEnum;
import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
import br.com.estudo.consorcio.domain.model.BemReferencia;
import br.com.estudo.consorcio.domain.model.CategoriaBem;
import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.domain.repository.BemReferenciaRepository;
import br.com.estudo.consorcio.domain.repository.CategoriaBemRepository;
import br.com.estudo.consorcio.domain.repository.ProdutoConsorcioRepository;
import br.com.estudo.consorcio.domain.repository.TipoVendaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Order(2)
public class VendasDataLoader implements CommandLineRunner {

    private final CategoriaBemRepository categoriaRepository;
    private final BemReferenciaRepository bemRepository;
    private final ProdutoConsorcioRepository produtoRepository;
    private final TipoVendaRepository tipoVendaRepository;

    public VendasDataLoader(CategoriaBemRepository categoriaRepository,
                            BemReferenciaRepository bemRepository,
                            ProdutoConsorcioRepository produtoRepository,
                            TipoVendaRepository tipoVendaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.bemRepository = bemRepository;
        this.produtoRepository = produtoRepository;
        this.tipoVendaRepository = tipoVendaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Garantir Bens de Referência
        if (bemRepository.count() == 0) {
            CategoriaBem catVeiculo = categoriaRepository.findByTipoBacen(TipoCategoriaBacen.BEM_MOVEL_I)
                    .orElseGet(() -> categoriaRepository.save(CategoriaBem.builder().nome("Veículos Automotores").tipoBacen(TipoCategoriaBacen.BEM_MOVEL_I).build()));
            CategoriaBem catImovel = categoriaRepository.findByTipoBacen(TipoCategoriaBacen.BEM_IMOVEL)
                    .orElseGet(() -> categoriaRepository.save(CategoriaBem.builder().nome("Imóveis").tipoBacen(TipoCategoriaBacen.BEM_IMOVEL).build()));

            BemReferencia bemAuto = bemRepository.save(BemReferencia.builder()
                    .categoriaBem(catVeiculo)
                    .descricao("Carro Popular 1.0 (Ex: Onix, HB20)")
                    .valorAtual(new BigDecimal("75000.00"))
                    .dataUltimaAtualizacao(LocalDate.now())
                    .ativo(true)
                    .build());

            BemReferencia bemImovel = bemRepository.save(BemReferencia.builder()
                    .categoriaBem(catImovel)
                    .descricao("Imóvel Residencial Padrão")
                    .valorAtual(new BigDecimal("350000.00"))
                    .dataUltimaAtualizacao(LocalDate.now())
                    .ativo(true)
                    .build());

            System.out.println("🌱 Seeded Bens de Referência padrão com sucesso!");
        }

        // 2. Garantir Produtos de Consórcio
        if (produtoRepository.count() == 0) {
            BemReferencia bem = bemRepository.findAll().stream().findFirst().orElse(null);

            produtoRepository.save(ProdutoConsorcio.builder()
                    .nome("Plano Auto 100 meses - Popular")
                    .bemReferencia(bem)
                    .prazoMeses(100)
                    .taxaAdministracaoPerc(new BigDecimal("15.00"))
                    .fundoReservaPerc(new BigDecimal("2.00"))
                    .ativo(true)
                    .build());

            produtoRepository.save(ProdutoConsorcio.builder()
                    .nome("Plano Auto 120 meses - Premium")
                    .bemReferencia(bem)
                    .prazoMeses(120)
                    .taxaAdministracaoPerc(new BigDecimal("12.00"))
                    .fundoReservaPerc(new BigDecimal("2.00"))
                    .ativo(true)
                    .build());

            produtoRepository.save(ProdutoConsorcio.builder()
                    .nome("Plano Imóvel 240 meses")
                    .bemReferencia(bem)
                    .prazoMeses(240)
                    .taxaAdministracaoPerc(new BigDecimal("20.00"))
                    .fundoReservaPerc(new BigDecimal("3.00"))
                    .ativo(true)
                    .build());

            System.out.println("🌱 Seeded Produtos de Consórcio padrão com sucesso!");
        }

        // 3. Garantir Tipos de Venda
        if (tipoVendaRepository.count() == 0) {
            TipoVenda tv = new TipoVenda();
            tv.setNome("Venda Direta");
            tv.setCanal(TipoVendaEnum.VENDA_DIRETA);
            tv.setPercentualComissao(new BigDecimal("0.05"));
            tv.setExigeSeguro(false);
            tv.setPermiteReajuste(true);
            tv.setAtivo(true);
            tv.setDataCriacao(LocalDateTime.now());
            tipoVendaRepository.save(tv);

            System.out.println("🌱 Seeded Tipos de Venda padrão com sucesso!");
        }
    }
}
