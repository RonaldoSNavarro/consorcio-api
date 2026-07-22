package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.ListaRestritiva;
import br.com.estudo.consorcio.domain.model.OrigemListaRestritiva;
import br.com.estudo.consorcio.domain.model.ComplianceExecucaoLog;
import br.com.estudo.consorcio.domain.repository.ListaRestritivaRepository;
import br.com.estudo.consorcio.repository.ComplianceExecucaoLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
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
    private final ComplianceExecucaoLogRepository logRepository;

    public ComplianceSincronizacaoService(ListaRestritivaRepository listaRestritivaRepository,
                                          MatchComplianceService matchComplianceService,
                                          ComplianceExecucaoLogRepository logRepository) {
        this.listaRestritivaRepository = listaRestritivaRepository;
        this.matchComplianceService = matchComplianceService;
        this.logRepository = logRepository;
    }

    @Async
    @Transactional
    public void sincronizarListas() {
        log.info("Iniciando sincronização completa de compliance...");
        long start = System.currentTimeMillis();
        
        ComplianceExecucaoLog execLog = new ComplianceExecucaoLog();
        execLog.setTriggerExecucao("MANUAL");
        execLog.setErros("[]");
        
        try {
            // 1. Integração com a API do OFAC
            int ofacCount = baixarEProcessarOfac(execLog);

            // 2. Mocks adicionais / Carga inicial padrão
            int onuCount = carregarMocksPadrao();
            execLog.setOnuRegistros(onuCount);
            execLog.setPepRegistros(1);
            execLog.setIbgeRegistros(0);

            // 3. Cruzamento de base de clientes
            matchComplianceService.cruzarBaseDeClientes();
            
            execLog.setDuracaoMs(System.currentTimeMillis() - start);
            logRepository.save(execLog);
            
            log.info("Sincronização completa de compliance finalizada.");
        } catch (Exception e) {
            log.error("Erro na sincronizacao: ", e);
            execLog.setErros("[\"" + e.getMessage() + "\"]");
            execLog.setDuracaoMs(System.currentTimeMillis() - start);
            logRepository.save(execLog);
        }
    }

    private int carregarMocksPadrao() {
        inserirOuAtualizarRegistro("OSAMA BIN LADEN", null, OrigemListaRestritiva.ONU);
        inserirOuAtualizarRegistro("POLITICO CORRUPTO SILVA", "111.222.333-44", OrigemListaRestritiva.PEP);
        return 1;
    }

    public int baixarEProcessarOfac(ComplianceExecucaoLog execLog) {
        try {
            log.info("Iniciando download e processamento de lista OFAC...");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
                    
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://sanctionslistservice.ofac.treas.gov/api/download/CONS_ADVANCED.XML"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(java.time.Duration.ofSeconds(60))
                    .GET()
                    .build();
                    
            java.net.http.HttpResponse<java.io.InputStream> response = client.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                    
            if (response.statusCode() == 200) {
                int count = processarOfacXml(response.body());
                log.info("Lista OFAC processada com sucesso: {} registros importados.", count);
                execLog.setOfacStatus("ONLINE");
                execLog.setOfacRegistros(count);
                return count;
            } else {
                log.warn("Falha no download da lista OFAC. HTTP Status: {}. Usando fallback local/mock.", response.statusCode());
                execLog.setOfacStatus("OFFLINE");
                execLog.setOfacRegistros(2);
                carregarMockOfacFallback();
                return 2;
            }
        } catch (Exception e) {
            log.warn("Erro ao integrar com a API do OFAC: {}. Usando fallback local/mock.", e.getMessage());
            execLog.setOfacStatus("OFFLINE");
            execLog.setOfacRegistros(2);
            carregarMockOfacFallback();
            return 2;
        }
    }

    private void carregarMockOfacFallback() {
        inserirOuAtualizarRegistro("JOHN DOE TERRORIST", null, OrigemListaRestritiva.OFAC);
        inserirOuAtualizarRegistro("RICARDO GARCIA SANCHES", "135.932.078-42", OrigemListaRestritiva.OFAC);
    }

    public int processarOfacXml(InputStream inputStream) throws Exception {
        int count = 0;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        boolean insideDocumentedName = false;
        boolean insideNamePartValue = false;
        StringBuilder fullNameBuilder = new StringBuilder();

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLEvent.START_ELEMENT:
                    String localName = reader.getLocalName();
                    if ("DocumentedName".equals(localName)) {
                        insideDocumentedName = true;
                        fullNameBuilder.setLength(0);
                    } else if ("NamePartValue".equals(localName) && insideDocumentedName) {
                        insideNamePartValue = true;
                    }
                    break;

                case XMLEvent.CHARACTERS:
                    if (insideNamePartValue) {
                        fullNameBuilder.append(reader.getText().trim()).append(" ");
                    }
                    break;

                case XMLEvent.END_ELEMENT:
                    String endName = reader.getLocalName();
                    if ("DocumentedName".equals(endName)) {
                        insideDocumentedName = false;
                        String fullName = fullNameBuilder.toString().trim();
                        if (!fullName.isEmpty()) {
                            inserirOuAtualizarRegistro(fullName.toUpperCase(), null, OrigemListaRestritiva.OFAC);
                            count++;
                        }
                    } else if ("NamePartValue".equals(endName)) {
                        insideNamePartValue = false;
                    }
                    break;
            }
        }
        reader.close();
        return count;
    }

    @Transactional
    public int processarPepCsv(InputStream inputStream) throws Exception {
        java.util.Set<String> existentes = listaRestritivaRepository
                .findAll().stream()
                .filter(l -> l.getOrigem() == OrigemListaRestritiva.PEP)
                .map(ListaRestritiva::getNome)
                .collect(java.util.stream.Collectors.toSet());

        int count = 0;
        java.util.List<ListaRestritiva> batch = new java.util.ArrayList<>();

        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.contains("ï¿½") || content.contains("\uFFFD")) {
            content = new String(bytes, StandardCharsets.ISO_8859_1);
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            boolean isHeader = true;
            String delimiter = ";";

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    if (!line.contains(";") && line.contains(",")) {
                        delimiter = ",";
                    } else if (line.contains("\t")) {
                        delimiter = "\t";
                    }
                    continue;
                }

                String[] parts = line.split(delimiter);
                if (parts.length >= 2) {
                    String cpf = parts[0].replaceAll("^\"|\"$", "").trim();
                    String nome = parts[1].replaceAll("^\"|\"$", "").trim().toUpperCase();

                    if (nome.length() < 2) continue;

                    if (!existentes.contains(nome)) {
                        ListaRestritiva novo = new ListaRestritiva();
                        novo.setNome(nome);
                        novo.setDocumentoOrigem(cpf);
                        novo.setOrigem(OrigemListaRestritiva.PEP);
                        novo.setDataInclusao(LocalDateTime.now());
                        batch.add(novo);
                        existentes.add(nome);
                        count++;

                        if (batch.size() >= 500) {
                            listaRestritivaRepository.saveAll(batch);
                            batch.clear();
                        }
                    }
                }
            }
            if (!batch.isEmpty()) {
                listaRestritivaRepository.saveAll(batch);
            }
        }
        log.info("Lista PEP processada: {} novos registros inseridos.", count);
        return count;
    }

    @Transactional
    public int processarOnuXml(InputStream inputStream) throws Exception {
        java.util.Set<String> existentes = listaRestritivaRepository
                .findAll().stream()
                .filter(l -> l.getOrigem() == OrigemListaRestritiva.ONU)
                .map(ListaRestritiva::getNome)
                .collect(java.util.stream.Collectors.toSet());

        int count = 0;
        java.util.List<ListaRestritiva> batch = new java.util.ArrayList<>();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);

        String currentElement = "";
        boolean inIndividual = false;
        boolean inEntity = false;
        boolean inDocument = false;

        String firstName = "";
        String secondName = "";
        String thirdName = "";
        String fourthName = "";
        String docNum = "";

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentElement = reader.getLocalName().toUpperCase();
                    if ("INDIVIDUAL".equals(currentElement)) {
                        inIndividual = true;
                        firstName = ""; secondName = ""; thirdName = ""; fourthName = ""; docNum = "";
                    } else if ("ENTITY".equals(currentElement)) {
                        inEntity = true;
                        firstName = "";
                    } else if ("INDIVIDUAL_DOCUMENT".equals(currentElement)) {
                        inDocument = true;
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (reader.isWhiteSpace()) break;
                    String text = reader.getText().trim();
                    if (text.isEmpty()) break;

                    if (inIndividual) {
                        if ("FIRST_NAME".equals(currentElement)) firstName += text;
                        else if ("SECOND_NAME".equals(currentElement)) secondName += text;
                        else if ("THIRD_NAME".equals(currentElement)) thirdName += text;
                        else if ("FOURTH_NAME".equals(currentElement)) fourthName += text;
                        else if (inDocument && "NUMBER".equals(currentElement)) docNum += text;
                    } else if (inEntity) {
                        if ("FIRST_NAME".equals(currentElement) || "NAME_ORIGINAL_SCRIPT".equals(currentElement)) firstName += text;
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    String endElement = reader.getLocalName().toUpperCase();
                    if ("INDIVIDUAL".equals(endElement)) {
                        StringBuilder fullNameBuilder = new StringBuilder();
                        if (!firstName.isEmpty()) fullNameBuilder.append(firstName).append(" ");
                        if (!secondName.isEmpty()) fullNameBuilder.append(secondName).append(" ");
                        if (!thirdName.isEmpty()) fullNameBuilder.append(thirdName).append(" ");
                        if (!fourthName.isEmpty()) fullNameBuilder.append(fourthName);

                        String fullName = fullNameBuilder.toString().trim().replaceAll("\\s+", " ").toUpperCase();
                        if (!fullName.isEmpty() && !existentes.contains(fullName)) {
                            ListaRestritiva novo = new ListaRestritiva();
                            novo.setNome(fullName);
                            novo.setDocumentoOrigem(docNum.isEmpty() ? null : docNum);
                            novo.setOrigem(OrigemListaRestritiva.ONU);
                            novo.setDataInclusao(LocalDateTime.now());
                            batch.add(novo);
                            existentes.add(fullName);
                            count++;

                            if (batch.size() >= 500) {
                                listaRestritivaRepository.saveAll(batch);
                                batch.clear();
                            }
                        }
                        inIndividual = false;
                    } else if ("ENTITY".equals(endElement)) {
                        String fullName = firstName.trim().replaceAll("\\s+", " ").toUpperCase();
                        if (!fullName.isEmpty() && !existentes.contains(fullName)) {
                            ListaRestritiva novo = new ListaRestritiva();
                            novo.setNome(fullName);
                            novo.setOrigem(OrigemListaRestritiva.ONU);
                            novo.setDataInclusao(LocalDateTime.now());
                            batch.add(novo);
                            existentes.add(fullName);
                            count++;

                            if (batch.size() >= 500) {
                                listaRestritivaRepository.saveAll(batch);
                                batch.clear();
                            }
                        }
                        inEntity = false;
                    } else if ("INDIVIDUAL_DOCUMENT".equals(endElement)) {
                        inDocument = false;
                    }
                    currentElement = "";
                    break;
            }
        }
        if (!batch.isEmpty()) {
            listaRestritivaRepository.saveAll(batch);
        }
        reader.close();
        log.info("Lista ONU processada: {} novos registros inseridos.", count);
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

    private void inserirOuAtualizarRegistro(String nome, String documento, OrigemListaRestritiva origem) {
        Optional<ListaRestritiva> existenteOpt = listaRestritivaRepository.findByNomeAndOrigem(nome, origem);
        if (existenteOpt.isPresent()) {
            ListaRestritiva existente = existenteOpt.get();
            existente.setDocumentoOrigem(documento);
            existente.setDataInclusao(LocalDateTime.now());
            listaRestritivaRepository.save(existente);
        } else {
            ListaRestritiva novo = new ListaRestritiva();
            novo.setNome(nome);
            novo.setDocumentoOrigem(documento);
            novo.setOrigem(origem);
            novo.setDataInclusao(LocalDateTime.now());
            listaRestritivaRepository.save(novo);
        }
    }
}
