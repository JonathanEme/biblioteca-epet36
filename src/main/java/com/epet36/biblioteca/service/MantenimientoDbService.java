package com.epet36.biblioteca.service;

import com.epet36.biblioteca.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MantenimientoDbService {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoDbService.class);

    private final PrestamoRepository prestamoRepository;
    // private final OutboxEventRepository outboxRepository; // (Descomentar si la limpieza es de Outbox)

    /**
     * Expresión CRON: "0 0 0 1 * ?"
     * Se ejecuta a las 00:00:00 del día 1 de cada mes.
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void limpiarTransaccionesCompletadas() {
        log.info("Iniciando tarea de mantenimiento mensual: Limpieza de transacciones completadas...");

        try {
            // Elimina todos los préstamos que ya fueron devueltos totalmente
            int eliminados = prestamoRepository.deleteByEstado("DEVUELTO");

            log.info("Mantenimiento finalizado. Se eliminaron {} préstamos completados de la base de datos local.", eliminados);

            // Si también querés limpiar eventos de auditoría u Outbox huérfanos, lo sumás acá:
            // int outboxEliminados = outboxRepository.deleteByProcessedTrue();

        } catch (Exception e) {
            log.error("Error durante la limpieza mensual de la base de datos", e);
        }
    }
}