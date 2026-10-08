package com.epet36.biblioteca.repository;

import com.epet36.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    // Para el dashboard: buscar préstamos activos o vencidos
    List<Prestamo> findByEstado(String estado);
    List<Prestamo> findByEstadoAndFechaVencimientoBefore(String estado, LocalDate fecha);
}