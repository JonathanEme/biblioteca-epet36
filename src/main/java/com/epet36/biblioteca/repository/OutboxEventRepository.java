package com.epet36.biblioteca.repository;

import com.epet36.biblioteca.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    // Este método es la clave del patrón: trae los eventos más viejos primero (FIFO).
    // Así garantizamos que si se acumulan varios cambios sin internet (ej: se crea y luego se edita un libro),
    // viajen a la nube exactamente en el mismo orden en que ocurrieron.
    List<OutboxEvent> findAllByOrderByCreatedAtAsc();
}
