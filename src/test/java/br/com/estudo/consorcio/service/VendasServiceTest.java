package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.BemReferencia;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.Corretor;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.ProdutoConsorcio;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.model.TipoVenda;
import br.com.estudo.consorcio.service.ComissaoVendaService;
import br.com.estudo.consorcio.service.ContabilidadeService;
import br.com.estudo.consorcio.domain.repository.ContratoAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.PropostaAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendasServiceTest {

    @Mock
    private PropostaAdesaoRepository propostaRepository;

    @Mock
    private ContratoAdesaoRepository contratoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ComissaoVendaService comissaoService;

    @Mock
    private ContabilidadeService contabilidadeService;

    @InjectMocks
    private VendasService vendasService;

    private Cliente cliente;
    private ProdutoConsorcio produto;
    private TipoVenda tipoVenda;
    private Corretor corretor;
    private PropostaAdesao proposta;
    private BemReferencia bem;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("João");

        bem = new BemReferencia();
        bem.setId(1L);
        bem.setValorAtual(BigDecimal.valueOf(50000));
        
        br.com.estudo.consorcio.domain.model.CategoriaBem catBem = new br.com.estudo.consorcio.domain.model.CategoriaBem();
        catBem.setTipoBacen(br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen.BEM_IMOVEL);
        bem.setCategoriaBem(catBem);

        produto = new ProdutoConsorcio();
        produto.setId(1L);
        produto.setBemReferencia(bem);
        produto.setPrazoMeses(60);
        produto.setTaxaAdministracaoPerc(BigDecimal.valueOf(0.15));

        tipoVenda = new TipoVenda();
        tipoVenda.setPercentualComissao(BigDecimal.valueOf(0.01));

        corretor = new Corretor();
        corretor.setId(1L);
        corretor.setNome("Corretor 1");

        proposta = new PropostaAdesao();
        proposta.setId(1L);
        proposta.setCliente(cliente);
        proposta.setProduto(produto);
        proposta.setTipoVenda(tipoVenda);
        proposta.setCorretor(corretor);
        proposta.setStatus(StatusProposta.EM_ANALISE);
        proposta.setValorCreditoSolicitado(BigDecimal.valueOf(50000));
    }

    @Test
    void aprovarProposta_DeveGerarContratoECotaAguardandoPagamento() {
        when(propostaRepository.findById(1L)).thenReturn(Optional.of(proposta));
        when(contratoRepository.save(any(ContratoAdesao.class))).thenAnswer(i -> i.getArgument(0));
        
        Grupo grupoExistente = new Grupo();
        grupoExistente.setId(1L);
        grupoExistente.setStatus(StatusGrupo.EM_ANDAMENTO);
        when(grupoRepository.encontrarMelhorGrupoDisponivel(any())).thenReturn(Optional.of(grupoExistente));
        
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArgument(0));

        ContratoAdesao contrato = vendasService.aprovarProposta(1L);

        assertEquals(StatusContrato.PENDENTE_PAGAMENTO, contrato.getStatus());
        assertEquals(StatusProposta.APROVADA, proposta.getStatus());
        
        verify(cotaRepository).save(argThat(cota -> cota.getStatus() == StatusCota.AGUARDANDO_PAGAMENTO));
    }

    @Test
    void efetivarPagamentoAdesao_DeveAtivarCotaEGerarComissao() {
        ContratoAdesao contrato = new ContratoAdesao();
        contrato.setId(1L);
        contrato.setProposta(proposta);
        contrato.setStatus(StatusContrato.PENDENTE_PAGAMENTO);
                
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));
        
        Cota cota = new Cota();
        cota.setId(1L);
        Grupo grupo = new Grupo();
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.AGUARDANDO_PAGAMENTO);
                
        when(cotaRepository.findByContratoAdesaoId(1L)).thenReturn(Optional.of(cota));

        vendasService.efetivarPagamentoAdesao(1L);

        assertEquals(StatusContrato.EFETIVADO, contrato.getStatus());
        assertEquals(StatusCota.ATIVA, cota.getStatus());
        
        verify(comissaoService, times(1)).criarComissaoPendente(any(), any(), any());
    }
}
