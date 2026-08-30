package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.ComissaoVenda;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.Corretor;
import br.com.estudo.consorcio.domain.repository.ComissaoVendaRepository;
import org.junit.jupiter.api.DisplayName;
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
class ComissaoVendaServiceTest {

    @Mock
    private ComissaoVendaRepository comissaoRepository;

    @InjectMocks
    private ComissaoVendaService comissaoService;

    @Test
    @DisplayName("Deve buscar comissão por contrato e status")
    void deveBuscarPorContratoEStatus() {
        ComissaoVenda comissao = ComissaoVenda.builder().id(1L).status("PENDENTE").build();
        when(comissaoRepository.findByContratoIdAndStatus(10L, "PENDENTE")).thenReturn(Optional.of(comissao));

        Optional<ComissaoVenda> resultado = comissaoService.buscarPorContratoEStatus(10L, "PENDENTE");

        assertTrue(resultado.isPresent());
        assertEquals("PENDENTE", resultado.get().getStatus());
    }

    @Test
    @DisplayName("Deve pagar comissão alterando status para PAGA")
    void devePagarComissao() {
        ComissaoVenda comissao = ComissaoVenda.builder().id(1L).status("PENDENTE").build();

        comissaoService.pagarComissao(comissao);

        assertEquals("PAGA", comissao.getStatus());
        verify(comissaoRepository).save(comissao);
    }

    @Test
    @DisplayName("Deve estornar comissão alterando status para ESTORNADA_PARCIALMENTE")
    void deveEstornarComissao() {
        ComissaoVenda comissao = ComissaoVenda.builder().id(1L).status("PAGA").build();

        comissaoService.estornarComissao(comissao);

        assertEquals("ESTORNADA_PARCIALMENTE", comissao.getStatus());
        verify(comissaoRepository).save(comissao);
    }

    @Test
    @DisplayName("Deve criar comissão pendente com dados corretos")
    void deveCriarComissaoPendente() {
        Corretor corretor = new Corretor();
        ContratoAdesao contrato = new ContratoAdesao();
        BigDecimal valor = new BigDecimal("1500.00");

        when(comissaoRepository.save(any(ComissaoVenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComissaoVenda criada = comissaoService.criarComissaoPendente(corretor, contrato, valor);

        assertNotNull(criada);
        assertEquals("PENDENTE", criada.getStatus());
        assertEquals(valor, criada.getValorTotalComissao());
        verify(comissaoRepository).save(any(ComissaoVenda.class));
    }
}