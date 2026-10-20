package com.epet36.biblioteca.repository;

import com.epet36.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, String> {

    // Buscamos los Activos y los Parciales
    @Query("SELECT DISTINCT p FROM Prestamo p " +
            "JOIN FETCH p.profesor " +
            "JOIN FETCH p.detalles d " +
            "JOIN FETCH d.libro " +
            "WHERE p.estado IN ('ACTIVO', 'PARCIAL')")
    List<Prestamo> findByEstadoActivoOParcialConDetalles();

    // Se usa JOIN FETCH para traer los detalles, libros y al profesor en un solo viaje
    @Query("SELECT DISTINCT p FROM Prestamo p " +
            "JOIN FETCH p.profesor " +
            "JOIN FETCH p.detalles d " +
            "JOIN FETCH d.libro " +
            "WHERE p.estado = :estado")
    List<Prestamo> findByEstadoConDetalles(@Param("estado") String estado);

    // Vencidos que están Activos o Parciales
    @Query("SELECT DISTINCT p FROM Prestamo p " +
            "JOIN FETCH p.profesor " +
            "JOIN FETCH p.detalles d " +
            "JOIN FETCH d.libro " +
            "WHERE p.estado IN ('ACTIVO', 'PARCIAL') AND p.fechaVencimiento < :fecha")
    List<Prestamo> findByEstadoAndFechaVencimientoBeforeConDetalles(@Param("fecha") LocalDate fecha);

    // QUITAMOS p.estado = 'ACTIVO' DE AQUÍ. Ahora busca en todo el historial.
    @Query("SELECT DISTINCT p FROM Prestamo p " +
            "JOIN FETCH p.profesor prof " +
            "JOIN FETCH p.detalles d " +
            "JOIN FETCH d.libro l " +
            "WHERE (" +
            "LOWER(prof.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(prof.apellido) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.materia) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.titulo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "CAST(p.fechaPrestamo AS string) LIKE CONCAT('%', :filtro, '%')) " +
            "ORDER BY p.fechaPrestamo DESC")
    List<Prestamo> buscarPrestamosMultifiltro(@Param("filtro") String filtro);

    @Query("SELECT DISTINCT p FROM Prestamo p " +
            "JOIN FETCH p.profesor " +
            "JOIN FETCH p.detalles d " +
            "JOIN FETCH d.libro " +
            "ORDER BY p.fechaPrestamo DESC")
    List<Prestamo> obtenerTodosOrdenadosPorFechaDesc();
}