package com.epet36.biblioteca.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prestamo_detalle")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PrestamoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @Builder.Default
    private Integer cantidad = 1;
}