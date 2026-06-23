package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

@Slf4j
@Service
public class ComplianceSincronizacaoService {

    private final ListaRestritivaRepository listaRestritivaRepository;
    private final MatchComplianceService matchComplianceService;

    public ComplianceSincronizacaoService(ListaRestritivaRepository listaRestritivaRepository,
                                          MatchComplianceService matchComplianceService) {
        this.listaRestritivaRepository = listaRestritivaRepository;
        this.matchComplianceService = matchComplianceService;
    }

    @Async
    @Transactional
    public void sincronizarListas() {
        log.info("Iniciando sincronização completa de compliance...");

        // 1. Integração com a API do OFAC
        baixarEProcessarOfac();

        // 2. Mocks adicionais / Carga inicial padrão
        carregarMocksPadrao();

        // 3. Cruzamento de base de clientes
        matchComplianceService.cruzarBaseDeClientes();
        
        log.info("Sincronização completa de compliance finalizada.");
    }

    private void carregarMocksPadrao() {
        inserirOuAtualizarRegistro("OSAMA BIN LADEN", null, OrigemListaRestritiva.ONU);
        inserirOuAtualizarRegistro("POLITICO CORRUPTO SILVA", "111.222.333-44", OrigemListaRestritiva.PEP);
    }

    public void baixarEProcessarOfac() {
        try {
            log.info("Iniciando download e processamento de lista OFAC...");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
                    
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://sanctionslistservice.ofac.treas.gov/api/download/CONS_ADVANCED.XML"))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .GET()
                    .build();
                    
            java.net.http.HttpResponse<java.io.InputStream> response = client.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                    
            if (response.statusCode() == 200) {
                int count = processarOfacXml(response.body());
                log.info("Lista OFAC processada com sucesso: {} registros importados.", count);
            } else {
                log.warn("Falha no download da lista OFAC. HTTP Status: {}. Usando fallback local/mock.", response.statusCode());
                carregarMockOfacFallback();
            }
        } catch (Exception e) {
            log.warn("Erro ao integrar com a API do OFAC: {}. Usando fallback local/mock.", e.getMessage());
            carregarMockOfacFallback();
        }
    }

    private void carregarMockOfacFallback() {
        inserirOuAtualizarRegistro("JOHN DOE TERRORIST", null, OrigemListaRestritiva.OFAC);
        inserirOuAtualizarRegistro("RICARDO GARCIA SANCHES", "135.932.078-42", OrigemListaRestritiva.OFAC);
    }

    public int processarOfacXml(InputStream inputStream) throws Exception {
        int count = 0;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        NodeList translations = doc.getElementsByTagName("translation");
        for (int i = 0; i < translations.getLength(); i++) {
            Node node = translations.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element transEl = (Element) node;
                String fullName = getXmlTagValue(transEl, "formattedFullName");
                if (!fullName.isEmpty()) {
                    inserirOuAtualizarRegistro(fullName.toUpperCase(), null, OrigemListaRestritiva.OFAC);
                    count++;
                }
            }
        }
        return count;
    }

    @Transactional
    public int processarPepCsv(InputStream inputStream) throws Exception {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    String cpf = parts[0].trim();
                    String nome = parts[1].trim().toUpperCase();
                    if (!nome.isEmpty()) {
                        inserirOuAtualizarRegistro(nome, cpf, OrigemListaRestritiva.PEP);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Transactional
    public int processarOnuXml(InputStream inputStream) throws Exception {
        int count = 0;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        // 1. Processar INDIVIDUALS
        NodeList individuals = doc.getElementsByTagName("INDIVIDUAL");
        for (int i = 0; i < individuals.getLength(); i++) {
            Node node = individuals.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                
                String firstName = getXmlTagValue(element, "FIRST_NAME");
                String secondName = getXmlTagValue(element, "SECOND_NAME");
                String thirdName = getXmlTagValue(element, "THIRD_NAME");
                String fourthName = getXmlTagValue(element, "FOURTH_NAME");
                
                StringBuilder fullNameBuilder = new StringBuilder();
                if (!firstName.isEmpty()) fullNameBuilder.append(firstName).append(" ");
                if (!secondName.isEmpty()) fullNameBuilder.append(secondName).append(" ");
                if (!thirdName.isEmpty()) fullNameBuilder.append(thirdName).append(" ");
                if (!fourthName.isEmpty()) fullNameBuilder.append(fourthName);
                
                String fullName = fullNameBuilder.toString().trim().replaceAll("\\s+", " ").toUpperCase();
                
                String docNum = "";
                NodeList docNodes = element.getElementsByTagName("INDIVIDUAL_DOCUMENT");
                if (docNodes.getLength() > 0 && docNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element docEl = (Element) docNodes.item(0);
                    docNum = getXmlTagValue(docEl, "NUMBER");
                }
                
                if (!fullName.isEmpty()) {
                    inserirOuAtualizarRegistro(fullName, docNum.isEmpty() ? null : docNum, OrigemListaRestritiva.ONU);
                    count++;
                }
            }
        }

        // 2. Processar ENTITIES
        NodeList entities = doc.getElementsByTagName("ENTITY");
        for (int i = 0; i < entities.getLength(); i++) {
            Node node = entities.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String firstName = getXmlTagValue(element, "FIRST_NAME");
                String fullName = firstName.trim().replaceAll("\\s+", " ").toUpperCase();
                
                if (!fullName.isEmpty()) {
                    inserirOuAtualizarRegistro(fullName, null, OrigemListaRestritiva.ONU);
                    count++;
                }
            }
        }
        
        return count;
    }

    @Transactional
    public int processarIbgeXls(InputStream inputStream) throws Exception {
        int count = 0;
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet("Faixa de Fronteira - Munic\u00edpio 2024");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            
            int rows = sheet.getPhysicalNumberOfRows();
            for (int r = 1; r < rows; r++) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    Cell munCell = row.getCell(1);
                    Cell ufCell = row.getCell(8);
                    Cell cidGemeaCell = row.getCell(17);
                    
                    if (munCell != null && ufCell != null) {
                        String munName = getCellValueAsString(munCell).trim();
                        String ufSigla = getCellValueAsString(ufCell).trim().toUpperCase();
                        String cidGemea = cidGemeaCell != null ? getCellValueAsString(cidGemeaCell).trim() : "";
                        
                        if (!munName.isEmpty() && !ufSigla.isEmpty()) {
                            String compositeName = munName.toUpperCase() + " - " + ufSigla;
                            String docOrigem = "IBGE:" + ufSigla + ":" + munName.toUpperCase();
                            if (!cidGemea.isEmpty()) {
                                docOrigem += ":GEMEA:" + cidGemea.toUpperCase();
                            }
                            
                            inserirOuAtualizarRegistro(compositeName, docOrigem, OrigemListaRestritiva.IBGE);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.format("%d", (long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private String getXmlTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getFirstChild() != null) {
                return node.getFirstChild().getNodeValue().trim();
            }
        }
        return "";
    }

    private void inserirOuAtualizarRegistro(String nome, String documento, OrigemListaRestritiva origem) {
        Optional<ListaRestritiva> existenteOpt = listaRestritivaRepository.findByNomeAndOrigem(nome, origem);
        if (existenteOpt.isPresent()) {
            ListaRestritiva existente = existenteOpt.get();
            existente.setDocumentoOrigem(documento);
            existente.setDataInclusao(LocalDateTime.now());
            listaRestritivaRepository.save(existente);
        } else {
            ListaRestritiva novo = new ListaRestritiva();
            novo.setNome(novo.getNome() != null ? novo.getNome() : nome); // avoid overriding name if not needed, but here it is simple
            novo.setNome(nome);
            novo.setDocumentoOrigem(documento);
            novo.setOrigem(origem);
            novo.setDataInclusao(LocalDateTime.now());
            listaRestritivaRepository.save(novo);
        }
    }
}
