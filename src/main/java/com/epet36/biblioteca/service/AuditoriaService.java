package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Auditoria;
import com.epet36.biblioteca.model.OutboxEvent;
import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.AuditoriaRepository;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registrar(String accion, String entidad, String entidadId, Usuario usuario, String detalle) {
        Auditoria auditoria = Auditoria.builder()
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .usuario(usuario)
                .detalle(detalle)
                .build();

        Auditoria auditoriaGuardada = auditoriaRepository.save(auditoria);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        registrarEventoOutbox(auditoriaGuardada, "INSERT");
    }

    // --- MÉTODO AUXILIAR PARA EL OUTBOX ---
    private void registrarEventoOutbox(Auditoria auditoria, String operacion) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(auditoria);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("Auditoria")
                    .aggregateId(auditoria.getId())
                    .operation(operacion)
                    .payload(jsonPayload)
                    .build();

            outboxRepository.save(evento);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar el evento Outbox para la auditoría ID: " + auditoria.getId(), e);
        }
    }
}