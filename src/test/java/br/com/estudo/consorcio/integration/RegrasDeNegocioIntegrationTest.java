package br.com.estudo.consorcio.integration;

import br.com.estudo.consorcio.domain.dto.*;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.*;
import br.com.estudo.consorcio.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // Evita carregar configs de prod e sobe o H2
class RegrasDeNegocioIntegrationTest {

    @Autowired
    private ClienteService clienteService;
    
    @Autowired
    private GrupoService grupoService;

    @Autowired
    private CotaService cotaService;

    @Autowired
    private ParcelaService parcelaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MovimentoFinanceiroRepository movimentoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ViaCepService viaCepService;

    private Long idCliente;
    private Long idGrupo;

    @BeforeEach
    void setUp() {
        // Mock do ViaCEP para evitar chamadas de rede externas e erros de PKIX no teste
        org.mockito.Mockito.when(viaCepService.buscarCep(org.mockito.Mockito.anyString()))
                .thenReturn(new br.com.estudo.consorcio.domain.dto.ViaCepResponseDTO(
                        "01001000", "Praça da Sé", "Apto 12", "Sé", "São Paulo", "SP", false
                ));

        // 1. Criar Cliente Mockado (CPF dinâmico para evitar colisão no banco H2)
        String cpf = String.valueOf(System.currentTimeMillis()).substring(2, 13);
        String email = "joao" + System.currentTimeMillis() + "@email.com";
        ClienteRequestDTO clienteDTO = new ClienteRequestDTO("João da Silva", cpf, email, "11999999999", "01001000", "123", "", new BigDecimal("150000.00"), new BigDecimal("5000.00"), NivelRisco.BAIXO);
        ClienteResponseDTO clienteSalvo = clienteService.salvar(clienteDTO);
        this.idCliente = clienteSalvo.id();

        // 2. Criar Grupo com crédito de 100k, 100 meses
        String code = "GRP-" + System.currentTimeMillis();
        GrupoRequestDTO grupoDTO = new GrupoRequestDTO(code, new BigDecimal("100000.00"), 100, new BigDecimal("10.00"));
        GrupoResponseDTO grupoSalvo = grupoService.salvar(grupoDTO);
        this.idGrupo = grupoSalvo.id();
        
        grupoService.inaugurar(this.idGrupo, LocalDate.now());
    }

    @Test
    @DisplayName("Integração Fundo de Reserva: O sistema deve provisionar a taxa perfeitamente na criação da cobrança (PrePersist)")
    void deveProvisionarFundoDeReservaAoGerarParcela() {
        CotaRequestDTO cotaReq = new CotaRequestDTO(1, this.idCliente, this.idGrupo);
        CotaResponseDTO cotaRes = cotaService.salvar(cotaReq);

        assertNotNull(cotaRes.id());
        
        // Simular o motor de faturamento gerando a 1ª parcela
        ParcelaRequestDTO parcelaReq = new ParcelaRequestDTO(
            cotaRes.id(), 
            1, 
            new BigDecimal("1000.00"), // Fundo Comum
            new BigDecimal("100.00"),  // Taxa Adm
            new BigDecimal("50.00"),   // Fundo de Reserva
            BigDecimal.ZERO,           // Seguro
            LocalDate.now().plusMonths(1)
        );
        
        ParcelaResponseDTO primeira = parcelaService.salvar(parcelaReq);
        
        assertEquals(new BigDecimal("1000.00"), primeira.valorFundoComum(), "O Fundo Comum provisionado está errado");
        assertEquals(new BigDecimal("50.00"), primeira.valorFundoReserva(), "O Fundo Reserva provisionado está errado");
        
        // O valor total do boleto deve ser matematicamente a soma exata: 1000 + 100 + 50 = 1150.00
        assertEquals(new BigDecimal("1150.00"), primeira.valorParcela(), "O JPA PrePersist deve ter somado os valores automaticamente no banco");
    }

    @Test
    @DisplayName("Integração LGPD + BCB: Exclusão de cliente deve inativar registro mas preservar fisicamente para auditoria")
    void deveApenasInativarPreservandoHistorico() {
        // ACT - Pedido de exclusão pelo usuário (Direito ao Esquecimento)
        clienteService.inativar(this.idCliente);
        
        // ASSERT - O Banco Central obriga a manter os dados se houver transações financeiras
        Optional<Cliente> clienteOpt = clienteRepository.findById(this.idCliente);
        
        assertTrue(clienteOpt.isPresent(), "O registro NÃO PODE ser apagado fisicamente (DELETE) devido a regulamentação do BCB");
        assertEquals(StatusCliente.INATIVO, clienteOpt.get().getStatus(), "O status deve estar como INATIVO");
        assertEquals("João da Silva", clienteOpt.get().getNome(), "Os dados físicos devem ser mantidos para expurgo apenas após a data limite regulatória");
    }
}
