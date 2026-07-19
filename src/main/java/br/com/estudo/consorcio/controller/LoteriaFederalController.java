package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.LoteriaFederalDTO;
import br.com.estudo.consorcio.domain.model.LoteriaFederal;
import br.com.estudo.consorcio.domain.repository.LoteriaFederalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loteria-federal")
@Tag(name = "Loteria Federal", description = "Endpoints para registro de resultados da Loteria Federal")
public class LoteriaFederalController {

    private final LoteriaFederalRepository repository;

    public LoteriaFederalController(LoteriaFederalRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Registra um novo extrato da Loteria Federal")
    @PreAuthorize("hasAuthority('MANAGE_GRUPOS')")
    @PostMapping
    public ResponseEntity<LoteriaFederalDTO> registrar(@RequestBody LoteriaFederalDTO dto) {
        LoteriaFederal model = new LoteriaFederal();
        model.setConcurso(dto.concurso());
        model.setDataSorteio(dto.dataSorteio());
        model.setPremio1(dto.premio1());
        model.setPremio2(dto.premio2());
        model.setPremio3(dto.premio3());
        model.setPremio4(dto.premio4());
        model.setPremio5(dto.premio5());
        
        model = repository.save(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(model));
    }

    @Operation(summary = "Lista todos os extratos da Loteria Federal")
    @PreAuthorize("hasAuthority('VIEW_GRUPOS')")
    @GetMapping
    public ResponseEntity<List<LoteriaFederalDTO>> listar() {
        List<LoteriaFederalDTO> list = repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private LoteriaFederalDTO toDTO(LoteriaFederal model) {
        return new LoteriaFederalDTO(
            model.getId(),
            model.getConcurso(),
            model.getDataSorteio(),
            model.getPremio1(),
            model.getPremio2(),
            model.getPremio3(),
            model.getPremio4(),
            model.getPremio5()
        );
    }
}
