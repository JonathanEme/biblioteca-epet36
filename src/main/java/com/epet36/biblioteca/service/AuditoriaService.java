package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Auditoria;
import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    @Transactional
    public void registrar(String accion, String entidad, Long entidadId, Usuario usuario, String detalle) {
        Auditoria auditoria = Auditoria.builder()
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .usuario(usuario)
                .detalle(detalle)
                .build();
        auditoriaRepository.save(auditoria);
    }
}