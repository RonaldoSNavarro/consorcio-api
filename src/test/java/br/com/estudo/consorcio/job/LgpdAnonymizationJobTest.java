package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LgpdAnonymizationJobTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private LgpdAnonymizationJob job;

    private Grupo grupoEncerradoAntigo;
    private Cliente cliente;
    private Cota cota;

    @BeforeEach
    void setUp() {
        grupoEncerradoAntigo = new Grupo();
        grupoEncerradoAntigo.setId(1L);
        grupoEncerradoAntigo.setStatus(StatusGrupo.ENCERRADO);
        grupoEncerradoAntigo.setDataEncerramento(LocalDate.now().minusYears(11));

        cliente = new Cliente();
        cliente.setId(100L);
        cliente.setNome("Carlos Silva");
        cliente.setCpfCnpj("12345678901");
        cliente.setEmail("carlos@exemplo.com");
        cliente.setTelefone("11999998888");

        cota = new Cota();
        cota.setId(10L);
        cota.setCliente(cliente);
        cota.setGrupo(grupoEncerradoAntigo);
    }

    @Test
    @DisplayName("Deve anonimizar clientes de grupos encerrados há mais de 10 anos")
    void deveAnonimizarClientesDeGruposEncerradosMaisDe10Anos() {
        when(grupoRepository.findByStatusAndDataEncerramentoBefore(eq(StatusGrupo.ENCERRADO), any(LocalDate.class)))
                .thenReturn(List.of(grupoEncerradoAntigo));
        when(cotaRepository.findByGrupoId(1L)).thenReturn(List.of(cota));

        job.executarExpurgoLgpd();

        verify(clienteRepository).save(argThat(c -> {
            assertTrue(c.getNome().startsWith("ANONIMIZADO-100"));
            assertEquals("00000000100", c.getCpfCnpj());
            assertEquals("anonimizado100@consorcio.local", c.getEmail());
            assertEquals("00000000000", c.getTelefone());
            assertEquals("EXPURGADO", c.getBairro());
            return true;
        }));

        verify(grupoRepository).save(argThat(g -> g.getStatus() == StatusGrupo.EXPURGADO));
    }

    @Test
    @DisplayName("Não deve processar grupos se nenhum estiver encerrado há mais de 10 anos")
    void naoDeveProcessarSemGruposElegiveis() {
        when(grupoRepository.findByStatusAndDataEncerramentoBefore(eq(StatusGrupo.ENCERRADO), any(LocalDate.class)))
                .thenReturn(List.of());

        job.executarExpurgoLgpd();

        verifyNoInteractions(cotaRepository);
        verifyNoInteractions(clienteRepository);
    }

    @Test
    @DisplayName("Não deve re-anonimizar cliente que já possua prefixo ANONIMIZADO")
    void naoDeveReAnonimizarClienteJaAnonimizado() {
        cliente.setNome("ANONIMIZADO-100");

        when(grupoRepository.findByStatusAndDataEncerramentoBefore(eq(StatusGrupo.ENCERRADO), any(LocalDate.class)))
                .thenReturn(List.of(grupoEncerradoAntigo));
        when(cotaRepository.findByGrupoId(1L)).thenReturn(List.of(cota));

        job.executarExpurgoLgpd();

        verify(clienteRepository, never()).save(any(Cliente.class));
        verify(grupoRepository).save(argThat(g -> g.getStatus() == StatusGrupo.EXPURGADO));
    }
}