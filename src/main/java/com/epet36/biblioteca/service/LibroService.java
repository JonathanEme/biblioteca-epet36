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

    public java.util.List<Libro> buscarSugerencias(String filtro) {
        // Limitamos a 10 resultados para que el menú emergente no sea muy pesado
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
        if (libroEditado.getId() == null) {
            // Es un alta manual directa al inventario
            libroEditado.setDisponibles(libroEditado.getCantidadTotal());
            return libroRepository.save(libroEditado);
        }

        Libro existente = buscarPorId(libroEditado.getId());

        // Actualizamos metadatos
        existente.setIsbn(libroEditado.getIsbn());
        existente.setCodigo(libroEditado.getCodigo());
        existente.setTitulo(libroEditado.getTitulo());
        existente.setAutor(libroEditado.getAutor());
        existente.setCategoria(libroEditado.getCategoria());

        // Ajuste inteligente de stock:
        // Si el bibliotecario dice que ahora hay 5 libros en total, y antes había 3.
        // La diferencia es +2. Por lo tanto, los disponibles también deben subir +2.
        int diferencia = libroEditado.getCantidadTotal() - existente.getCantidadTotal();
        int nuevaDisponibilidad = existente.getDisponibles() + diferencia;

        if (nuevaDisponibilidad < 0) {
            throw new IllegalStateException("No puedes reducir el stock total por debajo de la cantidad de libros que actualmente están prestados.");
        }

        existente.setCantidadTotal(libroEditado.getCantidadTotal());
        existente.setDisponibles(nuevaDisponibilidad);

        return libroRepository.save(existente);
    }
}