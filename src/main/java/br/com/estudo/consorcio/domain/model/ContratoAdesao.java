package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.StatusContrato;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contratos_adesao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoAdesao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroContrato;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id")
    private PropostaAdesao proposta;

    private LocalDateTime dataAssinatura;

    private String ipAssinatura;

    @Enumerated(EnumType.STRING)
    private StatusContrato status;
}
