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
    public Libro obtenerOCrearLibroParaPrestamo(Libro libroIngresado) {
        if (libroIngresado.getId() != null) {
            return buscarPorId(libroIngresado.getId());
        }

        // Buscar por ISBN o Código si fueron provistos
        if (libroIngresado.getIsbn() != null && !libroIngresado.getIsbn().isEmpty()) {
            var existente = libroRepository.findByIsbn(libroIngresado.getIsbn());
            if (existente.isPresent()) return existente.get();
        }

        if (libroIngresado.getCodigo() != null && !libroIngresado.getCodigo().isEmpty()) {
            var existente = libroRepository.findByCodigo(libroIngresado.getCodigo());
            if (existente.isPresent()) return existente.get();
        }

        // Búsqueda por título como red de seguridad
        var porTitulo = libroRepository.findByTituloIgnoreCase(libroIngresado.getTitulo());
        if (porTitulo.isPresent()) {
            return porTitulo.get();
        }

        // Es un libro nuevo: Inicializamos su stock
        libroIngresado.setCantidadTotal(1);
        libroIngresado.setDisponibles(1);
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