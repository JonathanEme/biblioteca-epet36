package com.epet36.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "prestamo_detalle")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PrestamoDetalle {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    // @JsonIgnore corta la relación hacia arriba en el JSON.
    // Cuando el Outbox guarde el Préstamo, guardará los detalles, pero los detalles no volverán a imprimir el préstamo.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    // Evita que Jackson intente serializar los objetos internos de control de Hibernate
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "cantidad_devuelta")
    @Builder.Default
    private Integer cantidadDevuelta = 0;
}