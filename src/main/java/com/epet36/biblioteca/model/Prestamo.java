package com.epet36.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prestamos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Prestamo {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString(); // Se agregó el punto y coma faltante

    // Ignoramos los proxies internos de Hibernate para evitar errores de LazyLoading en el JSON
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioAlta;

    @CreationTimestamp
    @Column(name = "fecha_prestamo", updatable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @Column(name = "materia")
    private String materia;

    @Column(nullable = false)
    @Builder.Default
    private String estado = "ACTIVO";

    // Evitamos el bucle infinito al serializar la relación bidireccional
    @JsonIgnoreProperties("prestamo")
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrestamoDetalle> detalles = new ArrayList<>();

    public void addDetalle(PrestamoDetalle detalle) {
        detalles.add(detalle);
        detalle.setPrestamo(this);
    }
}