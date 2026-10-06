package com.epet36.biblioteca.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String accion; // CREAR, ACTUALIZAR, ELIMINAR, PRESTAR, DEVOLVER

    @Column(nullable = false)
    private String entidad; // Libro, Profesor, Prestamo

    @Column(name = "entidad_id")
    private Long entidadId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fecha;

    private String detalle;
}