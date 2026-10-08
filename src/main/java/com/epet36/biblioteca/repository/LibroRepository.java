package com.epet36.biblioteca.repository;

import com.epet36.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {
    // Para buscar si el libro ya existe antes de crearlo "al vuelo"
    Optional<Libro> findByIsbn(String isbn);
    Optional<Libro> findByCodigo(String codigo);

    // Búsqueda por título exacto por si no usan ISBN/Código
    Optional<Libro> findByTituloIgnoreCase(String titulo);
}