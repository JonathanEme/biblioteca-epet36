package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.*;
import com.epet36.biblioteca.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;

    // Inyectamos Servicios, NO Repositorios ajenos
    private final ProfesorService profesorService;
    private final LibroService libroService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Prestamo registrarPrestamo(Long profesorId, Long usuarioId, Libro libroIngresado, LocalDate fechaVencimiento) {

        // 1. Obtener entidades relacionadas validadas
        Profesor profesor = profesorService.buscarPorId(profesorId);
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        // 2. Resolver el libro (buscar o crear)
        Libro libroFinal = libroService.obtenerOCrearLibroParaPrestamo(libroIngresado);

        // 3. Actualizar inventario (-1) -> Esto lanza excepción si no hay stock, haciendo rollback
        libroService.actualizarDisponibilidad(libroFinal.getId(), -1);

        // 4. Construir y guardar el préstamo
        Prestamo prestamo = Prestamo.builder()
                .profesor(profesor)
                .usuarioAlta(usuario)
                .fechaVencimiento(fechaVencimiento)
                .estado("ACTIVO")
                .build();

        PrestamoDetalle detalle = PrestamoDetalle.builder()
                .libro(libroFinal)
                .cantidad(1)
                .build();
        prestamo.addDetalle(detalle);

        Prestamo guardado = prestamoRepository.save(prestamo);

        // 5. Auditar la acción
        auditoriaService.registrar("PRESTAR", "Prestamo", guardado.getId(), usuario,
                "Préstamo registrado al profesor DNI: " + profesor.getDni() + " (Libro: " + libroFinal.getTitulo() + ")");

        return guardado;
    }

    @Transactional
    public Prestamo registrarDevolucion(Long prestamoId, Long usuarioId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado"));

        if (!"ACTIVO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo ya se encuentra devuelto o cerrado.");
        }

        // Actualizar estado del préstamo
        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucion(LocalDateTime.now());

        // Restaurar disponibilidad de todos los libros en el detalle (+1)
        for (PrestamoDetalle detalle : prestamo.getDetalles()) {
            libroService.actualizarDisponibilidad(detalle.getLibro().getId(), detalle.getCantidad());
        }

        Prestamo guardado = prestamoRepository.save(prestamo);

        // Auditar
        auditoriaService.registrar("DEVOLVER", "Prestamo", guardado.getId(), usuario,
                "Devolución procesada. Profesor: " + prestamo.getProfesor().getDni());

        return guardado;
    }
}