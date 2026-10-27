package com.epet36.biblioteca.repository;

import com.epet36.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, String> {
    // Para buscar si el libro ya existe antes de crearlo "al vuelo"
    Optional<Libro> findByIsbn(String isbn);
    Optional<Libro> findByCodigo(String codigo);

    // Búsqueda por título exacto por si no usan ISBN/Código
    Optional<Libro> findByTituloIgnoreCase(String titulo);

    // Búsqueda inteligente para el autocompletado
    @Query("SELECT l FROM Libro l WHERE " +
            "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.codigo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.autor) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    java.util.List<Libro> buscarParaAutocompletar(@org.springframework.data.repository.query.Param("filtro") String filtro);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Libro l WHERE " +
            "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.autor) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.codigo) LIKE LOWER(CONCAT('%', :filtro, '%')) ORDER BY l.titulo ASC")
    java.util.List<Libro> buscarInventario(@org.springframework.data.repository.query.Param("filtro") String filtro);
}