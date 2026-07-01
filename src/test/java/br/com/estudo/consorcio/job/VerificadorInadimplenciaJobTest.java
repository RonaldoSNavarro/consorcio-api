package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.service.CotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificadorInadimplenciaJobTest {

    @Mock
    private CotaRepository cotaRepository;
    @Mock
    private ParcelaRepository parcelaRepository;
    @Mock
    private CotaService cotaService;

    @InjectMocks
    private VerificadorInadimplenciaJob verificadorInadimplenciaJob;

    private Cota cotaAtiva;
    private Cota cotaContemplada;
    private Cota cotaSuspensa;

    @BeforeEach
    void setUp() {
        cotaAtiva = new Cota();
        cotaAtiva.setId(1L);
        cotaAtiva.setStatus(StatusCota.ATIVA);

        cotaContemplada = new Cota();
        cotaContemplada.setId(2L);
        cotaContemplada.setStatus(StatusCota.CONTEMPLADA);

        cotaSuspensa = new Cota();
        cotaSuspensa.setId(3L);
        cotaSuspensa.setStatus(StatusCota.SUSPENSA);
    }

    @Test
    void deveSuspenderCotaAtivaComUmaOuDuasParcelasAtrasadas() {
        when(cotaRepository.findAll()).thenReturn(List.of(cotaAtiva));
        
        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PENDENTE);
        p1.setDataVencimento(LocalDate.now().minusDays(5));
        
        when(parcelaRepository.findByCotaId(1L)).thenReturn(List.of(p1));

        verificadorInadimplenciaJob.verificarInadimplencia();

        verify(cotaService).registrarTransicaoVersao(eq(cotaAtiva), eq(StatusCota.SUSPENSA), anyString());
    }

    @Test
    void deveExcluirCotaAtivaComTresOuMaisParcelasAtrasadas() {
        when(cotaRepository.findAll()).thenReturn(List.of(cotaAtiva));
        
        Parcela p1 = new Parcela(); p1.setStatus(StatusParcela.PENDENTE); p1.setDataVencimento(LocalDate.now().minusDays(90));
        Parcela p2 = new Parcela(); p2.setStatus(StatusParcela.PENDENTE); p2.setDataVencimento(LocalDate.now().minusDays(60));
        Parcela p3 = new Parcela(); p3.setStatus(StatusParcela.PENDENTE); p3.setDataVencimento(LocalDate.now().minusDays(30));

        when(parcelaRepository.findByCotaId(1L)).thenReturn(List.of(p1, p2, p3));

        verificadorInadimplenciaJob.verificarInadimplencia();

        verify(cotaService).registrarTransicaoVersao(eq(cotaAtiva), eq(StatusCota.EXCLUIDA), anyString());
    }

    @Test
    void deveMandarParaExecucaoCotaContempladaComTresOuMaisParcelasAtrasadas() {
        when(cotaRepository.findAll()).thenReturn(List.of(cotaContemplada));
        
        Parcela p1 = new Parcela(); p1.setStatus(StatusParcela.PENDENTE); p1.setDataVencimento(LocalDate.now().minusDays(90));
        Parcela p2 = new Parcela(); p2.setStatus(StatusParcela.PENDENTE); p2.setDataVencimento(LocalDate.now().minusDays(60));
        Parcela p3 = new Parcela(); p3.setStatus(StatusParcela.PENDENTE); p3.setDataVencimento(LocalDate.now().minusDays(30));

        when(parcelaRepository.findByCotaId(2L)).thenReturn(List.of(p1, p2, p3));

        verificadorInadimplenciaJob.verificarInadimplencia();

        verify(cotaService).registrarTransicaoVersao(eq(cotaContemplada), eq(StatusCota.EM_EXECUCAO), anyString());
    }

    @Test
    void deveReativarCotaSuspensaSemParcelasAtrasadas() {
        when(cotaRepository.findAll()).thenReturn(List.of(cotaSuspensa));
        when(parcelaRepository.findByCotaId(3L)).thenReturn(List.of());

        verificadorInadimplenciaJob.verificarInadimplencia();

        verify(cotaService).registrarTransicaoVersao(eq(cotaSuspensa), eq(StatusCota.ATIVA), anyString());
    }
}
