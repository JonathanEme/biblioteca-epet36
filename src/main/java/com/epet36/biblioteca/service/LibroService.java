package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    public Libro buscarPorId(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado con ID: " + id));
    }

    @Transactional
    // Agregamos el parámetro cantidadRequerida
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

        // Si es un libro nuevo, su stock total y disponible inicial es igual a lo que están llevando
        libroIngresado.setCantidadTotal(cantidadRequerida);
        libroIngresado.setDisponibles(cantidadRequerida);
        return libroRepository.save(libroIngresado);
    }

    @Transactional
    public void actualizarDisponibilidad(Long id, int variacion) {
        Libro libro = buscarPorId(id);
        int nuevaDisponibilidad = libro.getDisponibles() + variacion;

        if (nuevaDisponibilidad < 0) {
            throw new IllegalStateException("No hay disponibilidad suficiente para el libro: " + libro.getTitulo());
        }

        libro.setDisponibles(nuevaDisponibilidad);
        libroRepository.save(libro);
    }
}