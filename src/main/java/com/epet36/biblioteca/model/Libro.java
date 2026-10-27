package com.epet36.biblioteca.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "libros")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Libro {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    // Podría ser el código de barras o inventario interno
    @Column(unique = true)
    private String codigo;

    private String isbn;

    @Column(nullable = false)
    private String titulo;

    private String autor;
    private String editorial;
    private String categoria;

    @Column(name = "anio_publicacion")
    private Integer anio;

    @Column(name = "cantidad_total")
    @Builder.Default
    private Integer cantidadTotal = 1;

    @Builder.Default
    private Integer disponibles = 1;

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}