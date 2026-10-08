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

    @Transactional
    public Profesor guardar(Profesor profesor) {
        // Aquí podrías agregar validaciones extra (ej. formato de DNI)
        if (profesor.getId() == null && profesorRepository.findByDni(profesor.getDni()).isPresent()) {
            throw new IllegalStateException("Ya existe un profesor registrado con el DNI: " + profesor.getDni());
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