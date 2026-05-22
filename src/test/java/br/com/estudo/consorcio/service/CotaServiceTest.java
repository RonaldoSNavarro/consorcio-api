package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotaServiceTest {

    @Mock
    private CotaRepository cotaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private GrupoRepository grupoRepository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.CotaMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.CotaMapper.class);

    @InjectMocks
    private CotaService service;

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um cliente inexistente")
    void deveLancarExcecaoClienteNaoEncontrado() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 99L, 1L); // Cliente 99 não existe

        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Cliente não encontrado.", exception.getMessage());
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar cota com sucesso e garantir o status ATIVA")
    void deveSalvarCotaComSucesso() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 2L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.ATIVO);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(grupoRepository.findById(2L)).thenReturn(Optional.of(grupo));

        // Simula o repositório devolvendo a cota salva
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArguments()[0]);

        // --- ACT ---
        var response = service.salvar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(15, response.numeroCota());
        assertEquals(StatusCota.ATIVA, response.status(), "A cota deve nascer obrigatoriamente ATIVA");
        verify(cotaRepository, times(1)).save(any(Cota.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um grupo inexistente")
    void deveLancarExcecaoGrupoNaoEncontrado() {
        // --- ARRANGE ---
        // Solicitamos a compra para o Cliente 1 no Grupo 99 (que não existe)
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 99L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.ATIVO);

        // Ensinamos o Mockito: O cliente existe e passa pela primeira validação
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        // Mas a segunda validação falha porque o grupo não existe
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        // Verificamos se ele estourou o erro correto no momento correto
        assertEquals("Grupo não encontrado.", exception.getMessage());

        // Garante que não tentou salvar no banco
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um cliente inativo")
    void deveLancarExcecaoAoSalvarCotaParaClienteInativo() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 2L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.INATIVO);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        // --- ACT & ASSERT ---
        assertThrows(ClienteInativoException.class, () -> service.salvar(request));

        verify(cotaRepository, never()).save(any());
        verify(grupoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve listar todas as cotas mapeadas para DTO")
    void deveListarTodasAsCotas() {
        // --- ARRANGE ---
        Cliente cliente = new Cliente(); cliente.setId(1L);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        Cota cota1 = new Cota(); cota1.setId(100L); cota1.setNumeroCota(10); cota1.setCliente(cliente); cota1.setGrupo(grupo); cota1.setStatus(StatusCota.ATIVA);
        Cota cota2 = new Cota(); cota2.setId(101L); cota2.setNumeroCota(20); cota2.setCliente(cliente); cota2.setGrupo(grupo); cota2.setStatus(StatusCota.ATIVA);

        when(cotaRepository.findAll()).thenReturn(List.of(cota1, cota2));

        // --- ACT ---
        List<CotaResponseDTO> resultado = service.listarTodas();

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(100L, resultado.get(0).id());
        assertEquals(10, resultado.get(0).numeroCota());
        assertEquals(1L, resultado.get(0).clienteId()); // Verifica se a conversão do Cliente extraiu o ID corretamente

        verify(cotaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve listar cotas filtradas por ID do Cliente")
    void deveListarCotasPorCliente() {
        // --- ARRANGE ---
        Long idClientePesquisado = 5L;
        Cliente cliente = new Cliente(); cliente.setId(idClientePesquisado);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        Cota cota = new Cota(); cota.setId(100L); cota.setCliente(cliente); cota.setGrupo(grupo);

        when(cotaRepository.findByClienteId(idClientePesquisado)).thenReturn(List.of(cota));

        // --- ACT ---
        List<CotaResponseDTO> resultado = service.listarPorCliente(idClientePesquisado);

        // --- ASSERT ---
        assertEquals(1, resultado.size());
        assertEquals(idClientePesquisado, resultado.get(0).clienteId());
        verify(cotaRepository, times(1)).findByClienteId(idClientePesquisado);
    }

    @Test
    @DisplayName("Deve listar cotas filtradas por ID do Grupo")
    void deveListarCotasPorGrupo() {
        // --- ARRANGE ---
        Long idGrupoPesquisado = 10L;
        Cliente cliente = new Cliente(); cliente.setId(1L);
        Grupo grupo = new Grupo(); grupo.setId(idGrupoPesquisado);

        Cota cota = new Cota(); cota.setId(100L); cota.setCliente(cliente); cota.setGrupo(grupo);

        when(cotaRepository.findByGrupoId(idGrupoPesquisado)).thenReturn(List.of(cota));

        // --- ACT ---
        List<CotaResponseDTO> resultado = service.listarPorGrupo(idGrupoPesquisado);

        // --- ASSERT ---
        assertEquals(1, resultado.size());
        assertEquals(idGrupoPesquisado, resultado.get(0).grupoId());
        verify(cotaRepository, times(1)).findByGrupoId(idGrupoPesquisado);
    }

    @Test
    @DisplayName("Deve retornar uma lista vazia caso não encontre cotas")
    void deveRetornarListaVaziaQuandoNaoHouverCotas() {
        // --- ARRANGE ---
        when(cotaRepository.findAll()).thenReturn(List.of()); // Simula o banco de dados vazio

        // --- ACT ---
        List<CotaResponseDTO> resultado = service.listarTodas();

        // --- ASSERT ---
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "A lista deve retornar vazia, não nula");
    }
}