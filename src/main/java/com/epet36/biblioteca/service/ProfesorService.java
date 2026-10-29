package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.OutboxEvent;
import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.epet36.biblioteca.repository.ProfesorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // CAMBIO: id ahora es String (UUID)
    public Profesor buscarPorId(String id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con ID: " + id));
    }

    // Método auxiliar para detectar si es un profesor nuevo o una edición
    public Optional<Profesor> buscarPorIdOpcional(String id) {
        return profesorRepository.findById(id);
    }

    public Profesor buscarPorDni(String dni) {
        return profesorRepository.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un profesor activo con el DNI: " + dni));
    }

    public java.util.List<Profesor> obtenerTodosActivos() {
        return profesorRepository.findByActivoTrueOrderByApellidoAscNombreAsc();
    }

    @Transactional
    public Profesor guardar(Profesor profesor) {
        boolean esNuevo = (profesor.getId() == null || buscarPorIdOpcional(profesor.getId()).isEmpty());

        if (esNuevo && profesor.getId() == null) {
            profesor.setId(UUID.randomUUID().toString()); // Aseguramos el UUID
        }

        // 1. Validar unicidad de Nombre y Apellido
        var existentePorNombre = profesorRepository.findByNombreIgnoreCaseAndApellidoIgnoreCase(profesor.getNombre(), profesor.getApellido());
        if (existentePorNombre.isPresent() && !existentePorNombre.get().getId().equals(profesor.getId())) {
            throw new IllegalStateException("Ya existe un profesor registrado como: " + profesor.getNombre() + " " + profesor.getApellido());
        }

        // 2. Controlar el DNI (convertir vacíos a nulos para que SQLite no tire error de Unique Constraint con strings vacíos)
        if (profesor.getDni() != null && profesor.getDni().trim().isEmpty()) {
            profesor.setDni(null);
        }

        // 3. Validar unicidad de DNI (solo si lo ingresaron)
        if (profesor.getDni() != null) {
            var existentePorDni = profesorRepository.findByDni(profesor.getDni());
            if (existentePorDni.isPresent() && !existentePorDni.get().getId().equals(profesor.getId())) {
                throw new IllegalStateException("Ya existe otro profesor registrado con el DNI: " + profesor.getDni());
            }
        }

        Profesor profesorGuardado = profesorRepository.save(profesor);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        registrarEventoOutbox(profesorGuardado, esNuevo ? "INSERT" : "UPDATE");

        return profesorGuardado;
    }

    @Transactional
    // CAMBIO: id ahora es String (UUID)
    public void desactivar(String id) {
        Profesor profesor = buscarPorId(id);
        profesor.setActivo(false);
        Profesor profesorGuardado = profesorRepository.save(profesor);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        // Desactivar es básicamente un UPDATE del campo 'activo' a false
        registrarEventoOutbox(profesorGuardado, "UPDATE");
    }

    // --- MÉTODO AUXILIAR PARA EL OUTBOX ---
    private void registrarEventoOutbox(Profesor profesor, String operacion) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(profesor);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("Profesor")
                    .aggregateId(profesor.getId())
                    .operation(operacion)
                    .payload(jsonPayload)
                    .build();

            outboxRepository.save(evento);
        } catch (Exception e) {
            // Si falla la serialización, se hace rollback y no se guarda en SQLite
            throw new RuntimeException("Error al serializar el evento Outbox para el profesor: " + profesor.getNombre(), e);
        }
    }

    /**
     * Guarda profesores provenientes de Turso en SQLite.
     * NO genera eventos Outbox.
     */
    @Transactional
    public void sincronizarDesdeNube(Profesor profesorDesdeTurso) {
        Optional<Profesor> existente = profesorRepository.findById(profesorDesdeTurso.getId());

        if (existente.isPresent()) {
            Profesor local = existente.get();
            local.setDni(profesorDesdeTurso.getDni());
            local.setNombre(profesorDesdeTurso.getNombre());
            local.setApellido(profesorDesdeTurso.getApellido());
            local.setEmailTelefono(profesorDesdeTurso.getEmailTelefono()); // o .setContacto(...)
            local.setActivo(profesorDesdeTurso.getActivo());

            profesorRepository.save(local);
        } else {
            profesorRepository.save(profesorDesdeTurso);
        }
    }
}