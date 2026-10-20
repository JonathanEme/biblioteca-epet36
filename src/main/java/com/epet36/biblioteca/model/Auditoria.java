package com.epet36.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Auditoria {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    // Blindamos la conversión a JSON evitando los proxies de Hibernate
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String accion; // CREAR, ACTUALIZAR, ELIMINAR, PRESTAR, DEVOLVER

    @Column(nullable = false)
    private String entidad; // Libro, Profesor, Prestamo

    // CAMBIO VITAL: Ahora guarda UUIDs de las otras entidades, por ende debe ser String
    @Column(name = "entidad_id")
    private String entidadId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fecha;

    private String detalle;
}