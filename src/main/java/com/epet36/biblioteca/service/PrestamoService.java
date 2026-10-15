package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.*;
import com.epet36.biblioteca.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;

    // Inyectamos Servicios, NO Repositorios ajenos
    private final ProfesorService profesorService;
    private final LibroService libroService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    // Actualizamos los parámetros
    @Transactional
    public Prestamo registrarPrestamo(Long profesorId, Long usuarioId, Libro libroIngresado, LocalDate fechaVencimiento, String materia, int cantidad) {

        Profesor profesor = profesorService.buscarPorId(profesorId);
        Usuario usuario = usuarioService.obtenerUsuarioPorDefecto();

        // PASO CLAVE: Le pasamos la cantidad al LibroService
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
                .cantidad(cantidad) // Guardamos la cantidad prestada
                .cantidadDevuelta(0)
                .build();
        prestamo.addDetalle(detalle);

        Prestamo guardado = prestamoRepository.save(prestamo);

        auditoriaService.registrar("PRESTAR", "Prestamo", guardado.getId(), usuario,
                "Préstamo de " + cantidad + " libro(s) a profesor: " + profesor.getApellido() + " para materia: " + materia);

        return guardado;
    }

    public List<Prestamo> buscarPrestamosMultifiltro(String filtro) {
        return prestamoRepository.buscarPrestamosMultifiltro(filtro);
    }

    public List<Prestamo> obtenerTodosLosPrestamos() {
        return prestamoRepository.obtenerTodosOrdenadosPorFechaDesc();
    }

    @Transactional
    public Prestamo registrarDevolucion(Long prestamoId, Long usuarioId, int cantidadADevolver) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado"));

        if ("DEVUELTO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo ya se encuentra completamente devuelto.");
        }

        PrestamoDetalle detalle = prestamo.getDetalles().get(0); // Para el MVP asumimos 1 libro por préstamo
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
            // No se pone fecha_devolucion porque aún quedan libros afuera
            auditoriaService.registrar("DEVOLVER_PARCIAL", "Prestamo", prestamo.getId(), usuario,
                    "Devolución PARCIAL de " + cantidadADevolver + " libro(s). (" + detalle.getLibro().getTitulo() + ")");
        }

        return prestamoRepository.save(prestamo);
    }
}