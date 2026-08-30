package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "preferencias_notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false)
    @Builder.Default
    private Boolean habilitado = true;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}