package br.com.estudo.consorcio.job;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.service.GrupoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReajusteAniversarioGrupoJobTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private GrupoService grupoService;

    @InjectMocks
    private ReajusteAniversarioGrupoJob job;

    private Grupo grupoIpca;
    private Grupo grupoManual;

    @BeforeEach
    void setUp() {
        grupoIpca = new Grupo();
        grupoIpca.setId(10L);
        grupoIpca.setCodigo("GRP-IPCA");
        grupoIpca.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupoIpca.setIndiceReajuste(IndiceReajuste.IPCA);
        grupoIpca.setMesReajuste(LocalDate.now().getMonthValue());

        grupoManual = new Grupo();
        grupoManual.setId(20L);
        grupoManual.setCodigo("GRP-MANUAL");
        grupoManual.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupoManual.setIndiceReajuste(IndiceReajuste.MANUAL);
        grupoManual.setMesReajuste(LocalDate.now().getMonthValue());
    }

    @Test
    @DisplayName("Deve reajustar grupos em andamento que façam aniversário no mês atual e tenham índice automático")
    void deveReajustarGruposComIndiceAutomaticoNoMesAtual() {
        int mesAtual = LocalDate.now().getMonthValue();
        when(grupoRepository.findByStatusAndMesReajuste(eq(StatusGrupo.EM_ANDAMENTO), eq(mesAtual)))
                .thenReturn(List.of(grupoIpca));

        job.executarReajustesAniversario();

        verify(grupoService).reajustarGrupoPorIndice(10L, IndiceReajuste.IPCA);
    }

    @Test
    @DisplayName("Não deve reajustar automaticamente grupos com índice MANUAL ou nulo")
    void naoDeveReajustarGruposComIndiceManualOuNulo() {
        int mesAtual = LocalDate.now().getMonthValue();
        Grupo grupoSemIndice = new Grupo();
        grupoSemIndice.setId(30L);
        grupoSemIndice.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupoSemIndice.setIndiceReajuste(null);

        when(grupoRepository.findByStatusAndMesReajuste(eq(StatusGrupo.EM_ANDAMENTO), eq(mesAtual)))
                .thenReturn(List.of(grupoManual, grupoSemIndice));

        job.executarReajustesAniversario();

        verify(grupoService, never()).reajustarGrupoPorIndice(anyLong(), any(IndiceReajuste.class));
    }

    @Test
    @DisplayName("Deve continuar a execução mesmo se um grupo falhar ao ser reajustado")
    void deveContinuarExecucaoAposErroEmUmGrupo() {
        int mesAtual = LocalDate.now().getMonthValue();
        Grupo grupoIncc = new Grupo();
        grupoIncc.setId(40L);
        grupoIncc.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupoIncc.setIndiceReajuste(IndiceReajuste.INCC);

        when(grupoRepository.findByStatusAndMesReajuste(eq(StatusGrupo.EM_ANDAMENTO), eq(mesAtual)))
                .thenReturn(List.of(grupoIpca, grupoIncc));

        doThrow(new RuntimeException("Falha na API SGS")).when(grupoService).reajustarGrupoPorIndice(10L, IndiceReajuste.IPCA);

        job.executarReajustesAniversario();

        verify(grupoService).reajustarGrupoPorIndice(10L, IndiceReajuste.IPCA);
        verify(grupoService).reajustarGrupoPorIndice(40L, IndiceReajuste.INCC);
    }
}