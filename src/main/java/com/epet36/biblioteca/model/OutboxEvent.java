package com.epet36.biblioteca.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String aggregateType; // "Libro", "Profesor", "Usuario", "Prestamo", "Auditoria"
    private String aggregateId;
    private String operation;     // "INSERT", "UPDATE", "DELETE"

    @Lob
    private String payload;

    @Builder.Default
    @Column(name = "intentos")
    private Integer intentos = 0;

    @Column(name = "ultimo_error")
    private String ultimoError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}