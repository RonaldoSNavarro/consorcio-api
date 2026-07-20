package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.model.StatusAlertaCompliance;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComplianceServiceTest {

    private ListaRestritivaRepository listaRestritivaRepository;
    private MatchComplianceService matchComplianceService;
    private AlertaComplianceRepository alertaComplianceRepository;
    private ClienteRepository clienteRepository;
    private ComplianceSincronizacaoService sincronizacaoService;

    @BeforeEach
    public void setUp() {
        listaRestritivaRepository = mock(ListaRestritivaRepository.class);
        alertaComplianceRepository = mock(AlertaComplianceRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        matchComplianceService = new MatchComplianceService(clienteRepository, listaRestritivaRepository, alertaComplianceRepository);
        br.com.estudo.consorcio.repository.ComplianceExecucaoLogRepository logRepository = mock(br.com.estudo.consorcio.repository.ComplianceExecucaoLogRepository.class);
        sincronizacaoService = new ComplianceSincronizacaoService(listaRestritivaRepository, matchComplianceService, logRepository);
    }

    @Test
    public void testProcessarPepCsv() throws Exception {
        String csvContent = "CPF;Nome_PEP;Sigla_Função;Descrição_Função;Nível_Função;Nome_Órgão;Data_Início_Exercício;Data_Fim_Exercício;Data_Fim_Carência\n" +
                "***.531.324-**;MARCELO ANTÔNIO CARREIRA;CAS-1;DIR. SUPERINTENDENTE;;SUDEMA;21/12/2019;Não informada;Não informada";
        
        InputStream is = new java.io.ByteArrayInputStream(csvContent.getBytes("ISO-8859-1"));

        when(listaRestritivaRepository.findByNomeAndOrigem(anyString(), any(OrigemListaRestritiva.class)))
                .thenReturn(Optional.empty());

        int count = sincronizacaoService.processarPepCsv(is);

        assertEquals(1, count);
        
        ArgumentCaptor<java.util.List<ListaRestritiva>> captor = ArgumentCaptor.forClass(java.util.List.class);
        verify(listaRestritivaRepository, times(1)).saveAll(captor.capture());
        
        assertEquals(1, captor.getValue().size());
        ListaRestritiva saved = (ListaRestritiva) captor.getValue().get(0);
        assertEquals("MARCELO ANTÔNIO CARREIRA", saved.getNome());
        assertEquals("***.531.324-**", saved.getDocumentoOrigem());
        assertEquals(OrigemListaRestritiva.PEP, saved.getOrigem());
    }

    @Test
    public void testProcessarOnuXml() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<CONSOLIDATED_LIST>\n" +
                "  <INDIVIDUALS>\n" +
                "    <INDIVIDUAL>\n" +
                "      <FIRST_NAME>GEDO</FIRST_NAME>\n" +
                "      <SECOND_NAME>HAMDAN</SECOND_NAME>\n" +
                "      <THIRD_NAME>AHMED</THIRD_NAME>\n" +
                "      <INDIVIDUAL_DOCUMENT>\n" +
                "        <NUMBER>123456</NUMBER>\n" +
                "      </INDIVIDUAL_DOCUMENT>\n" +
                "    </INDIVIDUAL>\n" +
                "  </INDIVIDUALS>\n" +
                "  <ENTITIES>\n" +
                "    <ENTITY>\n" +
                "      <FIRST_NAME>AL-QAIDA</FIRST_NAME>\n" +
                "    </ENTITY>\n" +
                "  </ENTITIES>\n" +
                "</CONSOLIDATED_LIST>";

        InputStream is = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));

        when(listaRestritivaRepository.findByNomeAndOrigem(anyString(), any(OrigemListaRestritiva.class)))
                .thenReturn(Optional.empty());

        int count = sincronizacaoService.processarOnuXml(is);

        assertEquals(2, count);
        verify(listaRestritivaRepository, times(2)).save(any(ListaRestritiva.class));
    }

    @Test
    public void testProcessarIbgeXls() throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Faixa de Fronteira - Munic\u00edpio 2024");
        
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("CD_MUN");
        header.createCell(1).setCellValue("NM_MUN");
        header.createCell(8).setCellValue("SIGLA_UF");
        header.createCell(17).setCellValue("CID_GEMEA");

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("1100015");
        data.createCell(1).setCellValue("Alta Floresta D'Oeste");
        data.createCell(8).setCellValue("RO");
        data.createCell(17).setCellValue("SIM");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        InputStream is = new ByteArrayInputStream(bos.toByteArray());

        when(listaRestritivaRepository.findByNomeAndOrigem(anyString(), any(OrigemListaRestritiva.class)))
                .thenReturn(Optional.empty());

        int count = sincronizacaoService.processarIbgeXls(is);

        assertEquals(1, count);

        ArgumentCaptor<ListaRestritiva> captor = ArgumentCaptor.forClass(ListaRestritiva.class);
        verify(listaRestritivaRepository, times(1)).save(captor.capture());
        
        ListaRestritiva saved = captor.getValue();
        assertEquals("ALTA FLORESTA D'OESTE - RO", saved.getNome());
        assertEquals("IBGE:RO:ALTA FLORESTA D'OESTE:GEMEA:SIM", saved.getDocumentoOrigem());
        assertEquals(OrigemListaRestritiva.IBGE, saved.getOrigem());
    }

    @Test
    public void testCruzarBaseDeClientes_PepMatch() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Marcelo Antonio Carreira");
        cliente.setCpfCnpj("11153132488");
        cliente.setLocalidade("Sao Paulo");
        cliente.setUf("SP");

        ListaRestritiva pep = new ListaRestritiva();
        pep.setId(10L);
        pep.setNome("MARCELO ANTONIO CARREIRA");
        pep.setDocumentoOrigem("***.531.324-**");
        pep.setOrigem(OrigemListaRestritiva.PEP);

        AlertaComplianceRepository.MatchResultProjection match = new AlertaComplianceRepository.MatchResultProjection() {
            public Long getClienteId() { return 1L; }
            public Long getListaId() { return 10L; }
            public Double getScore() { return 1.0; }
        };

        when(alertaComplianceRepository.findPepMatches(anyDouble())).thenReturn(List.of(match));
        when(clienteRepository.getReferenceById(1L)).thenReturn(cliente);
        when(listaRestritivaRepository.getReferenceById(10L)).thenReturn(pep);

        matchComplianceService.cruzarBaseDeClientes();

        verify(alertaComplianceRepository).saveAll(any());
    }

    @Test
    public void testCruzarBaseDeClientes_IbgeMatch() {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setNome("Pedro Silva");
        cliente.setCpfCnpj("22233344455");
        cliente.setLocalidade("Alta Floresta D'Oeste");
        cliente.setUf("RO");

        ListaRestritiva ibge = new ListaRestritiva();
        ibge.setId(20L);
        ibge.setNome("ALTA FLORESTA D'OESTE - RO");
        ibge.setDocumentoOrigem("IBGE:RO:ALTA FLORESTA D'OESTE");
        ibge.setOrigem(OrigemListaRestritiva.IBGE);

        AlertaComplianceRepository.MatchResultProjection match = new AlertaComplianceRepository.MatchResultProjection() {
            public Long getClienteId() { return 2L; }
            public Long getListaId() { return 20L; }
            public Double getScore() { return 1.0; }
        };

        when(alertaComplianceRepository.findIbgeMatches()).thenReturn(List.of(match));
        when(clienteRepository.getReferenceById(2L)).thenReturn(cliente);
        when(listaRestritivaRepository.getReferenceById(20L)).thenReturn(ibge);

        matchComplianceService.cruzarBaseDeClientes();

        verify(alertaComplianceRepository).saveAll(any());
    }

    @ParameterizedTest
    @CsvSource({
        "Osama Bin Laden, OSAMA BIN LADEN, true",
        "Osama Bin Laden, OSAMA BIN LADIN, true",
        "Osama Bin Laden, OSAMA BEN LADEN, true",
        "Osama Bin Laden, OSAMA BIN LADEN JR, true",
        "Osama Bin Laden, GEORGE BUSH, false"
    })
    public void testJaroWinklerNameSimilarity(String name1, String name2, boolean shouldMatch) {
        org.apache.commons.text.similarity.JaroWinklerSimilarity jaro = new org.apache.commons.text.similarity.JaroWinklerSimilarity();
        double score = jaro.apply(normalizar(name1), normalizar(name2));
        boolean match = score >= 0.90;
        assertEquals(shouldMatch, match, "Comparison between '" + name1 + "' and '" + name2 + "' score: " + score);
    }

    private String normalizar(String str) {
        if (str == null) return "";
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toUpperCase();
    }
}
