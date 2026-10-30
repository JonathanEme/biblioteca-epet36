package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.event.OutboxEvent;
import com.epet36.biblioteca.repository.LibroRepository;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public Libro buscarPorId(String id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado con ID: " + id));
    }

    public Optional<Libro> buscarPorIdOpcional(String id) {
        return libroRepository.findById(id);
    }

    @Transactional
    public Libro obtenerOCrearLibroParaPrestamo(Libro libroIngresado, int cantidadRequerida) {
        if (libroIngresado.getId() != null) {
            return buscarPorId(libroIngresado.getId());
        }

        if (libroIngresado.getIsbn() != null && !libroIngresado.getIsbn().isEmpty()) {
            var existente = libroRepository.findByIsbn(libroIngresado.getIsbn());
            if (existente.isPresent()) return existente.get();
        }

        if (libroIngresado.getCodigo() != null && !libroIngresado.getCodigo().isEmpty()) {
            var existente = libroRepository.findByCodigo(libroIngresado.getCodigo());
            if (existente.isPresent()) return existente.get();
        }

        var porTitulo = libroRepository.findByTituloIgnoreCase(libroIngresado.getTitulo());
        if (porTitulo.isPresent()) {
            return porTitulo.get();
        }

        if (libroIngresado.getId() == null) {
            libroIngresado.setId(UUID.randomUUID().toString());
        }

        libroIngresado.setCantidadTotal(cantidadRequerida);
        libroIngresado.setDisponibles(cantidadRequerida);

        Libro libroGuardado = libroRepository.save(libroIngresado);
        registrarEventoOutbox(libroGuardado, "INSERT");

        return libroGuardado;
    }

    @Transactional
    public void actualizarDisponibilidad(String id, int variacion) {
        Libro libro = buscarPorId(id);
        int nuevaDisponibilidad = libro.getDisponibles() + variacion;

        if (nuevaDisponibilidad < 0) {
            throw new IllegalStateException("No hay disponibilidad suficiente para el libro: " + libro.getTitulo());
        }

        libro.setDisponibles(nuevaDisponibilidad);
        Libro libroGuardado = libroRepository.save(libro);
        registrarEventoOutbox(libroGuardado, "UPDATE");
    }

    public java.util.List<Libro> buscarSugerencias(String filtro) {
        return libroRepository.buscarParaAutocompletar(filtro)
                .stream()
                .limit(10)
                .toList();
    }

    public java.util.List<Libro> obtenerInventario(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return libroRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "titulo"));
        }
        return libroRepository.buscarInventario(filtro);
    }

    @Transactional
    public Libro actualizarLibroMantenimiento(Libro libroEditado) {
        Libro libroGuardado;
        boolean esNuevo = (libroEditado.getId() == null || buscarPorIdOpcional(libroEditado.getId()).isEmpty());

        if (esNuevo) {
            if (libroEditado.getId() == null) {
                libroEditado.setId(UUID.randomUUID().toString());
            }
            libroEditado.setDisponibles(libroEditado.getCantidadTotal());
            libroGuardado = libroRepository.save(libroEditado);
        } else {
            Libro existente = buscarPorId(libroEditado.getId());

            // Actualització de metadades (inclosos els nous camps)
            existente.setIsbn(libroEditado.getIsbn());
            existente.setCodigo(libroEditado.getCodigo());
            existente.setTitulo(libroEditado.getTitulo());
            existente.setAutor(libroEditado.getAutor());
            existente.setCategoria(libroEditado.getCategoria());
            existente.setEditorial(libroEditado.getEditorial());
            existente.setAnio(libroEditado.getAnio());

            int diferencia = libroEditado.getCantidadTotal() - existente.getCantidadTotal();
            int nuevaDisponibilidad = existente.getDisponibles() + diferencia;

            if (nuevaDisponibilidad < 0) {
                throw new IllegalStateException("No puedes reducir el stock total por debajo de la cantidad de libros que actualmente están prestados.");
            }

            existente.setCantidadTotal(libroEditado.getCantidadTotal());
            existente.setDisponibles(nuevaDisponibilidad);

            libroGuardado = libroRepository.save(existente);
        }

        registrarEventoOutbox(libroGuardado, esNuevo ? "INSERT" : "UPDATE");

        return libroGuardado;
    }

    private void registrarEventoOutbox(Libro libro, String operacion) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(libro);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("Libro")
                    .aggregateId(libro.getId())
                    .operation(operacion)
                    .payload(jsonPayload)
                    .build();

            outboxRepository.save(evento);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar el evento Outbox para el libro: " + libro.getTitulo(), e);
        }
    }

    /**
     * Guarda registros provenientes de Turso en la base de datos local SQLite.
     * NO genera eventos Outbox para evitar bucles infinitos de sincronización.
     */
    /**
     * Guarda libros provenientes de Turso en la base de datos local SQLite.
     * NO genera eventos Outbox. Previene duplicados por Código o ISBN.
     */
    @Transactional
    public void sincronizarDesdeNube(Libro libroDesdeTurso) {
        // 1. Buscamos primero por ID
        Optional<Libro> existente = libroRepository.findById(libroDesdeTurso.getId());

        // 2. Búsqueda cruzada por código interno
        if (existente.isEmpty() && libroDesdeTurso.getCodigo() != null && !libroDesdeTurso.getCodigo().isBlank()) {
            existente = libroRepository.findByCodigo(libroDesdeTurso.getCodigo());
        }

        // 3. Búsqueda cruzada por ISBN
        if (existente.isEmpty() && libroDesdeTurso.getIsbn() != null && !libroDesdeTurso.getIsbn().isBlank()) {
            existente = libroRepository.findByIsbn(libroDesdeTurso.getIsbn());
        }

        if (existente.isPresent()) {
            Libro local = existente.get();
            // Unificamos forzando el ID de Turso
            local.setCodigo(libroDesdeTurso.getCodigo());
            local.setIsbn(libroDesdeTurso.getIsbn());
            local.setTitulo(libroDesdeTurso.getTitulo());
            local.setAutor(libroDesdeTurso.getAutor());
            local.setEditorial(libroDesdeTurso.getEditorial());
            local.setAnio(libroDesdeTurso.getAnio());
            local.setCategoria(libroDesdeTurso.getCategoria());
            local.setCantidadTotal(libroDesdeTurso.getCantidadTotal());
            local.setDisponibles(libroDesdeTurso.getDisponibles());
            local.setActivo(libroDesdeTurso.getActivo());

            libroRepository.save(local);
        } else {
            libroRepository.save(libroDesdeTurso);
        }
    }
}