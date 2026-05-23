package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @Mock
    private ViaCepService viaCepService;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.ClienteMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.ClienteMapper.class);

    @InjectMocks
    private ClienteService service;

    @Test
    @DisplayName("Deve salvar cliente com sucesso quando dados são únicos")
    void deveSalvarClienteComSucesso() {
        // --- ARRANGE ---
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ronaldo", 
                "12345678909", 
                "ronaldo@email.com", 
                "13999999999",
                "11001000",
                "123",
                "Apto 1",
                new BigDecimal("100000.00"),
                new BigDecimal("5000.00"),
                NivelRisco.MEDIO
        );

        ViaCepResponseDTO viaCepMock = new ViaCepResponseDTO(
                "11001000", 
                "Rua das Flores", 
                "Apto 1", 
                "Centro", 
                "Santos", 
                "SP", 
                false
        );

        when(viaCepService.buscarCep("11001000")).thenReturn(viaCepMock);
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
        assertEquals("Ron***", response.nome());
        verify(repository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o CPF/CNPJ já estiver cadastrado")
    void deveLancarExcecaoCpfDuplicado() {
        // --- ARRANGE ---
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Ronaldo", 
                "12345678909", 
                "ronaldo@email.com", 
                "13999999999",
                "11001000",
                "123",
                "Apto 1",
                new BigDecimal("100000.00"),
                new BigDecimal("5000.00"),
                NivelRisco.MEDIO
        );

        // Simulamos que o repositório JÁ ENCONTRA um cliente com esse CPF
        when(repository.findByCpfCnpj(request.cpfCnpj())).thenReturn(Optional.of(new Cliente()));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Já existe um cliente cadastrado com o documento: 12345678909", exception.getMessage());
        verify(repository, never()).save(any()); // Garante que NÃO salvou
    }

    @Test
    @DisplayName("Deve listar todos os clientes convertendo para DTO")
    void deveListarClientes() {
        Cliente c1 = new Cliente(); 
        c1.setId(1L); 
        c1.setNome("Cliente 1");
        c1.setCep("11001000");
        c1.setLogradouro("Rua A");
        c1.setNumero("10");
        c1.setBairro("Bairro A");
        c1.setLocalidade("Cidade A");
        c1.setUf("SP");
        c1.setPatrimonio(new BigDecimal("10000.00"));
        c1.setRendaMensal(new BigDecimal("2000.00"));
        c1.setNivelRisco(NivelRisco.BAIXO);
        c1.setDataCadastro(LocalDate.now());

        Cliente c2 = new Cliente(); 
        c2.setId(2L); 
        c2.setNome("Cliente 2");
        c2.setCep("11001000");
        c2.setLogradouro("Rua B");
        c2.setNumero("20");
        c2.setBairro("Bairro B");
        c2.setLocalidade("Cidade B");
        c2.setUf("SP");
        c2.setPatrimonio(new BigDecimal("20000.00"));
        c2.setRendaMensal(new BigDecimal("4000.00"));
        c2.setNivelRisco(NivelRisco.MEDIO);
        c2.setDataCadastro(LocalDate.now());

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        when(repository.findAll(pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c1, c2)));

        org.springframework.data.domain.Page<ClienteResponseDTO> resultado = service.listarTodos(pageable);

        assertEquals(2, resultado.getContent().size());
        assertEquals("Cl*** 1*", resultado.getContent().get(0).nome());
        assertEquals(1L, resultado.getContent().get(0).id());
        verify(repository, times(1)).findAll(pageable);
    }
}