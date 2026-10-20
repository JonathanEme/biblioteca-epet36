package com.epet36.biblioteca.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "profesores", uniqueConstraints = {
        // Aseguramos que no haya dos profesores con el mismo nombre y apellido
        @UniqueConstraint(columnNames = {"nombre", "apellido"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Profesor {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    // Quitamos nullable = false. Sigue siendo unique por si lo cargan.
    @Column(unique = true)
    private String dni;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(name = "email_telefono")
    private String emailTelefono;

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}