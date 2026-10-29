package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.*;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.epet36.biblioteca.repository.PrestamoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private final ProfesorService profesorService;
    private final LibroService libroService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Prestamo registrarPrestamo(String profesorId, String usuarioId, Libro libroIngresado, LocalDate fechaVencimiento, String materia, int cantidad) {

        Profesor profesor = profesorService.buscarPorId(profesorId);
        Usuario usuario = usuarioService.obtenerUsuarioPorDefecto();

        // PASO CLAVE: Le pasamos la cantidad al LibroService
        // Nota: Como LibroService también implementa Outbox, esto generará su propio evento para Turso.
        Libro libroFinal = libroService.obtenerOCrearLibroParaPrestamo(libroIngresado, cantidad);

        // Descontamos la cantidad prestada (ahora el resultado siempre será >= 0)
        libroService.actualizarDisponibilidad(libroFinal.getId(), -cantidad);

        Prestamo prestamo = Prestamo.builder()
                .profesor(profesor)
                .usuarioAlta(usuario)
                .fechaVencimiento(fechaVencimiento)
                .materia(materia)
                .estado("ACTIVO")
                .build();

        PrestamoDetalle detalle = PrestamoDetalle.builder()
                .libro(libroFinal)
                .cantidad(cantidad)
                .cantidadDevuelta(0)
                .build();
        prestamo.addDetalle(detalle);

        Prestamo guardado = prestamoRepository.save(prestamo);

        auditoriaService.registrar("PRESTAR", "Prestamo", guardado.getId(), usuario,
                "Préstamo de " + cantidad + " libro(s) a profesor: " + profesor.getApellido() + " para materia: " + materia);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        registrarEventoOutbox(guardado, "INSERT");

        return guardado;
    }

    public List<Prestamo> buscarPrestamosMultifiltro(String filtro) {
        return prestamoRepository.buscarPrestamosMultifiltro(filtro);
    }

    public List<Prestamo> obtenerTodosLosPrestamos() {
        return prestamoRepository.obtenerTodosOrdenadosPorFechaDesc();
    }

    @Transactional
    // CAMBIO: prestamoId y usuarioId ahora son String (UUID)
    public Prestamo registrarDevolucion(String prestamoId, String usuarioId, int cantidadADevolver) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado"));

        if ("DEVUELTO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo ya se encuentra completamente devuelto.");
        }

        PrestamoDetalle detalle = prestamo.getDetalles().get(0);
        int devueltosActuales = detalle.getCantidadDevuelta() == null ? 0 : detalle.getCantidadDevuelta();
        int cantidadPendiente = detalle.getCantidad() - devueltosActuales;

        if (cantidadADevolver <= 0 || cantidadADevolver > cantidadPendiente) {
            throw new IllegalArgumentException("Cantidad a devolver inválida. Quedan " + cantidadPendiente + " pendientes.");
        }

        // 1. Sumamos la nueva devolución al contador
        detalle.setCantidadDevuelta(devueltosActuales + cantidadADevolver);

        // 2. Restauramos la disponibilidad física del libro
        libroService.actualizarDisponibilidad(detalle.getLibro().getId(), cantidadADevolver);

        // 3. Evaluamos el nuevo estado del préstamo
        if (detalle.getCantidadDevuelta().equals(detalle.getCantidad())) {
            prestamo.setEstado("DEVUELTO");
            prestamo.setFechaDevolucion(LocalDateTime.now());
            auditoriaService.registrar("DEVOLVER_TOTAL", "Prestamo", prestamo.getId(), usuario,
                    "Devolución TOTAL completada. (" + detalle.getLibro().getTitulo() + ")");
        } else {
            prestamo.setEstado("PARCIAL");
            auditoriaService.registrar("DEVOLVER_PARCIAL", "Prestamo", prestamo.getId(), usuario,
                    "Devolución PARCIAL de " + cantidadADevolver + " libro(s). (" + detalle.getLibro().getTitulo() + ")");
        }

        Prestamo prestamoGuardado = prestamoRepository.save(prestamo);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        registrarEventoOutbox(prestamoGuardado, "UPDATE");

        return prestamoGuardado;
    }

    // --- MÉTODO AUXILIAR PARA EL OUTBOX ---
    private void registrarEventoOutbox(Prestamo prestamo, String operacion) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(prestamo);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("Prestamo")
                    .aggregateId(prestamo.getId())
                    .operation(operacion)
                    .payload(jsonPayload)
                    .build();

            outboxRepository.save(evento);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar el evento Outbox para el préstamo ID: " + prestamo.getId(), e);
        }
    }

    /**
     * Guarda préstamos provenientes de Turso en SQLite.
     * NO genera eventos Outbox.
     */
    @Transactional
    public void sincronizarDesdeNube(Prestamo prestamoDesdeTurso) {
        Optional<Prestamo> existente = prestamoRepository.findById(prestamoDesdeTurso.getId());

        if (existente.isPresent()) {
            Prestamo local = existente.get();
            local.setProfesor(prestamoDesdeTurso.getProfesor());
            local.setMateria(prestamoDesdeTurso.getMateria());
            local.setFechaPrestamo(prestamoDesdeTurso.getFechaPrestamo());
            local.setFechaVencimiento(prestamoDesdeTurso.getFechaVencimiento());
            local.setFechaDevolucion(prestamoDesdeTurso.getFechaDevolucion());
            local.setEstado(prestamoDesdeTurso.getEstado());

            // Sincronizar los detalles (PrestamoDetalle)
            if (local.getDetalles() != null && prestamoDesdeTurso.getDetalles() != null) {
                local.getDetalles().clear(); // Limpiamos los detalles actuales
                // Agregamos los que vienen de la nube
                local.getDetalles().addAll(prestamoDesdeTurso.getDetalles());
            }

            prestamoRepository.save(local);
        } else {
            prestamoRepository.save(prestamoDesdeTurso);
        }
    }
}