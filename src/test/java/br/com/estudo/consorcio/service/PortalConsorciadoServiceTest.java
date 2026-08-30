package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.PortalCotaDTO;
import br.com.estudo.consorcio.domain.dto.PortalExtratoItemDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalConsorciadoServiceTest {

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private LanceService lanceService;

    @InjectMocks
    private PortalConsorciadoService service;

    private Cliente cliente;
    private Grupo grupo;
    private Cota cota;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Consorciada");
        cliente.setCpfCnpj("12345678900");

        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setCodigoGrupo("GRP-100");
        grupo.setValorCredito(new BigDecimal("80000.00"));
        grupo.setPrazoMeses(60);

        cota = new Cota();
        cota.setId(100L);
        cota.setCodigoCota(15);
        cota.setStatus(StatusCota.ATIVA);
        cota.setCliente(cliente);
        cota.setGrupo(grupo);
    }

    @Test
    @DisplayName("Deve listar cotas pertencentes ao consorciado")
    void deveListarCotasDoCliente() {
        when(clienteRepository.findByCpfCnpj("12345678900")).thenReturn(Optional.of(cliente));
        when(cotaRepository.findByClienteId(1L)).thenReturn(List.of(cota));
        when(parcelaRepository.findByCotaId(100L)).thenReturn(List.of());

        List<PortalCotaDTO> cotas = service.listarCotasDoCliente("12345678900");

        assertNotNull(cotas);
        assertEquals(1, cotas.size());
        assertEquals(15, cotas.get(0).codigoCota());
        assertEquals("GRP-100", cotas.get(0).codigoGrupo());
    }

    @Test
    @DisplayName("Deve obter extrato de parcelas da cota com sucesso")
    void deveObterExtratoCota() {
        Parcela p1 = new Parcela();
        p1.setId(1L);
        p1.setNumeroParcela(1);
        p1.setDataVencimento(LocalDate.now().minusMonths(1));
        p1.setValorParcela(new BigDecimal("1500.00"));
        p1.setValorPago(new BigDecimal("1500.00"));
        p1.setStatus(StatusParcela.PAGA);

        when(cotaRepository.findById(100L)).thenReturn(Optional.of(cota));
        when(parcelaRepository.findByCotaId(100L)).thenReturn(List.of(p1));

        List<PortalExtratoItemDTO> extrato = service.obterExtratoCota(100L, "12345678900");

        assertNotNull(extrato);
        assertEquals(1, extrato.size());
        assertEquals(StatusParcela.PAGA, extrato.get(0).status());
    }

    @Test
    @DisplayName("Deve bloquear extrato quando documento não coincide com titular da cota (Ownership Guard)")
    void deveBloquearExtratoInvasivo() {
        when(cotaRepository.findById(100L)).thenReturn(Optional.of(cota));

        assertThrows(RegraDeNegocioException.class, () -> service.obterExtratoCota(100L, "99988877700"));
    }
}