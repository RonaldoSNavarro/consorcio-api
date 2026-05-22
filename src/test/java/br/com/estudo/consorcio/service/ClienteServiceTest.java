package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.ClienteMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.ClienteMapper.class);

    @InjectMocks
    private ClienteService service;

    @Test
    @DisplayName("Deve salvar cliente com sucesso quando dados são únicos")
    void deveSalvarClienteComSucesso() {
        // --- ARRANGE ---
        ClienteRequestDTO request = new ClienteRequestDTO("Ronaldo", "12345678901", "ronaldo@email.com", "1399999999");

        // Simulamos que não encontra nada no banco (vazio)
        when(repository.findByCpfCnpj(request.cpfCnpj())).thenReturn(Optional.empty());

        // Simulamos o save retornando a entidade com ID e Data
        when(repository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            c.setId(1L);
            c.setDataCadastro(LocalDate.from(LocalDateTime.now()));
            return c;
        });

        // --- ACT ---
        ClienteResponseDTO response = service.salvar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Ronaldo", response.nome());
        verify(repository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o CPF/CNPJ já estiver cadastrado")
    void deveLancarExcecaoCpfDuplicado() {
        // --- ARRANGE ---
        ClienteRequestDTO request = new ClienteRequestDTO("Ronaldo", "12345678901", "ronaldo@email.com", "139");

        // Simulamos que o repositório JÁ ENCONTRA um cliente com esse CPF
        when(repository.findByCpfCnpj(request.cpfCnpj())).thenReturn(Optional.of(new Cliente()));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Já existe um cliente cadastrado com o documento: 12345678901", exception.getMessage());
        verify(repository, never()).save(any()); // Garante que NÃO salvou
    }

    @Test
    @DisplayName("Deve listar todos os clientes convertendo para DTO")
    void deveListarClientes() {
        Cliente c1 = new Cliente(); c1.setId(1L); c1.setNome("Cliente 1");
        Cliente c2 = new Cliente(); c2.setId(2L); c2.setNome("Cliente 2");
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        when(repository.findAll(pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c1, c2)));

        org.springframework.data.domain.Page<ClienteResponseDTO> resultado = service.listarTodos(pageable);

        assertEquals(2, resultado.getContent().size());
        assertEquals("Cliente 1", resultado.getContent().get(0).nome());
        assertEquals(1L, resultado.getContent().get(0).id());
        verify(repository, times(1)).findAll(pageable);
    }
}