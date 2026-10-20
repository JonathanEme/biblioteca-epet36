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

    private String aggregateType; // Ejemplo: "Libro"
    private String aggregateId;   // El UUID del libro afectado
    private String operation;     // "INSERT", "UPDATE", "DELETE"

    @Lob // Para que SQLite permita guardar textos largos
    private String payload;       // El JSON del libro con todos sus datos

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}