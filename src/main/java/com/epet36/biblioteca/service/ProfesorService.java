package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfesorService {

    private final ProfesorRepository profesorRepository;

    public Profesor buscarPorId(Long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con ID: " + id));
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

        return profesorRepository.save(profesor);
    }

    @Transactional
    public void desactivar(Long id) {
        Profesor profesor = buscarPorId(id);
        profesor.setActivo(false);
        profesorRepository.save(profesor);
    }
}