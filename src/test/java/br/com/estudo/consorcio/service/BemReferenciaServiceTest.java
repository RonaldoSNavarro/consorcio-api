package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.BemReferenciaRequestDTO;
import br.com.estudo.consorcio.domain.dto.BemReferenciaResponseDTO;
import br.com.estudo.consorcio.domain.model.BemReferencia;
import br.com.estudo.consorcio.domain.model.CategoriaBem;
import br.com.estudo.consorcio.domain.model.HistoricoValorBemReferencia;
import br.com.estudo.consorcio.domain.repository.BemReferenciaRepository;
import br.com.estudo.consorcio.domain.repository.CategoriaBemRepository;
import br.com.estudo.consorcio.domain.repository.HistoricoValorBemReferenciaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BemReferenciaServiceTest {

    @Mock
    private BemReferenciaRepository repository;

    @Mock
    private CategoriaBemRepository categoriaRepository;

    @Mock
    private HistoricoValorBemReferenciaRepository historicoRepository;

    @InjectMocks
    private BemReferenciaService service;

    private CategoriaBem categoria;
    private BemReferencia bem;

    @BeforeEach
    void setUp() {
        categoria = new CategoriaBem();
        categoria.setId(1L);
        categoria.setNome("Automóveis");

        bem = BemReferencia.builder()
                .id(10L)
                .categoriaBem(categoria)
                .descricao("Honda Civic Touring")
                .valorAtual(new BigDecimal("180000.00"))
                .codigoFipe("004380-0")
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve salvar novo bem de referência e criar registro de histórico inicial")
    void deveSalvarBemComHistoricoInicial() {
        BemReferenciaRequestDTO dto = new BemReferenciaRequestDTO(
                1L, "Honda Civic Touring", new BigDecimal("180000.00"), "004380-0", true
        );

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.save(any(BemReferencia.class))).thenAnswer(i -> {
            BemReferencia b = i.getArgument(0);
            b.setId(10L);
            return b;
        });

        BemReferenciaResponseDTO res = service.salvar(dto);

        assertNotNull(res);
        assertEquals("Honda Civic Touring", res.descricao());
        verify(historicoRepository).save(any(HistoricoValorBemReferencia.class));
    }

    @Test
    @DisplayName("Deve atualizar bem de referência e registrar histórico de reajuste se o valor mudar")
    void deveAtualizarBemComHistoricoDeReajuste() {
        BemReferenciaRequestDTO dto = new BemReferenciaRequestDTO(
                1L, "Honda Civic Touring 2026", new BigDecimal("195000.00"), "004380-0", true
        );

        when(repository.findById(10L)).thenReturn(Optional.of(bem));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.save(any(BemReferencia.class))).thenAnswer(i -> i.getArgument(0));

        BemReferenciaResponseDTO res = service.atualizar(10L, dto, "REAJUSTE_FIPE");

        assertNotNull(res);
        assertEquals(new BigDecimal("195000.00"), res.valorAtual());
        verify(historicoRepository).save(argThat(h ->
                h.getValorAnterior().compareTo(new BigDecimal("180000.00")) == 0 &&
                h.getValorNovo().compareTo(new BigDecimal("195000.00")) == 0 &&
                "REAJUSTE_FIPE".equals(h.getOrigemReajuste())
        ));
    }

    @Test
    @DisplayName("Deve obter bem por ID")
    void deveObterPorId() {
        when(repository.findById(10L)).thenReturn(Optional.of(bem));

        BemReferenciaResponseDTO res = service.obterPorId(10L);

        assertEquals(10L, res.id());
        assertEquals("Honda Civic Touring", res.descricao());
    }

    @Test
    @DisplayName("Deve lançar erro ao buscar bem inexistente")
    void deveLancarErroAoBuscarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class, () -> service.obterPorId(99L));
    }

    @Test
    @DisplayName("Deve listar bens paginados")
    void deveListarPaginado() {
        when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(bem)));

        Page<BemReferenciaResponseDTO> page = service.listar(null, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }
}