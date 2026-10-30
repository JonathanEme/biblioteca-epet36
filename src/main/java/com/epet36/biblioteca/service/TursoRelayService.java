package com.epet36.biblioteca.service;

import com.epet36.biblioteca.event.OutboxEvent;
import com.epet36.biblioteca.model.*;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TursoRelayService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${turso.url:}")
    private String tursoUrl;

    @Value("${turso.token:}")
    private String tursoToken;
    

    private static final int MAX_INTENTOS = 5;

    // Cliente HTTP nativo de Java 11+
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Scheduled(fixedDelay = 30000)
    public void procesarEventosOutbox() {
        if (tursoUrl == null || tursoUrl.isBlank() || tursoToken == null || tursoToken.isBlank()) {
            return;
        }

        List<OutboxEvent> eventosPendientes = outboxRepository.findAllByOrderByCreatedAtAsc();
        if (eventosPendientes.isEmpty()) {
            return;
        }

        try {
            // 1. Verificamos/creamos las tablas en Turso mediante HTTP
            inicializarEsquemaTurso();

            // 2. Despachamos los eventos pendientes
            for (OutboxEvent evento : eventosPendientes) {
                if (evento.getIntentos() >= MAX_INTENTOS) {
                    continue;
                }

                try {
                    despacharEvento(evento);
                    outboxRepository.delete(evento); // Eliminación local tras confirmación de Turso
                } catch (Exception e) {
                    log.error("Fallo al procesar evento Outbox ID {}: {}", evento.getId(), e.getMessage());

                    evento.setIntentos(evento.getIntentos() + 1);
                    evento.setUltimoError(e.getMessage());
                    outboxRepository.save(evento);
                    return; // Interrumpe para reintentar en el próximo ciclo
                }
            }
            log.info("Sincronización Outbox completada con éxito en Turso.");

        } catch (Exception e) {
            log.warn("Turso inalcanzable. Se reintentará en el próximo ciclo. Detalle: {}",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private void inicializarEsquemaTurso() throws Exception {
        List<String> ddlStatements = List.of(
                "CREATE TABLE IF NOT EXISTS usuarios (id TEXT PRIMARY KEY, username TEXT, password_hash TEXT, nombre TEXT, rol TEXT, activo INTEGER)",
                "CREATE TABLE IF NOT EXISTS profesores (id TEXT PRIMARY KEY, dni TEXT, nombre TEXT, apellido TEXT, email_telefono TEXT, activo INTEGER)",
                "CREATE TABLE IF NOT EXISTS libros (id TEXT PRIMARY KEY, codigo TEXT, isbn TEXT, titulo TEXT, autor TEXT, editorial TEXT, categoria TEXT, anio_publicacion INTEGER, cantidad_total INTEGER, disponibles INTEGER, activo INTEGER)",
                "CREATE TABLE IF NOT EXISTS prestamos (id TEXT PRIMARY KEY, profesor_id TEXT, usuario_id TEXT, fecha_vencimiento TEXT, fecha_devolucion TEXT, materia TEXT, estado TEXT)",
                "CREATE TABLE IF NOT EXISTS prestamo_detalle (id TEXT PRIMARY KEY, prestamo_id TEXT, libro_id TEXT, cantidad INTEGER, cantidad_devuelta INTEGER)",
                "CREATE TABLE IF NOT EXISTS auditoria (id TEXT PRIMARY KEY, usuario_id TEXT, accion TEXT, entidad TEXT, entidad_id TEXT, detalle TEXT)"
        );

        for (String sql : ddlStatements) {
            ejecutarSqlEnTurso(sql, Collections.emptyList());
        }
    }

    private void despacharEvento(OutboxEvent evento) throws Exception {
        switch (evento.getAggregateType()) {
            case "Libro" -> sincronizarLibro(objectMapper.readValue(evento.getPayload(), Libro.class));
            case "Profesor" -> sincronizarProfesor(objectMapper.readValue(evento.getPayload(), Profesor.class));
            case "Usuario" -> sincronizarUsuario(objectMapper.readValue(evento.getPayload(), Usuario.class));
            case "Prestamo" -> sincronizarPrestamo(objectMapper.readValue(evento.getPayload(), Prestamo.class));
            case "Auditoria" -> sincronizarAuditoria(objectMapper.readValue(evento.getPayload(), Auditoria.class));
            default -> throw new IllegalArgumentException("Tipo de entidad no soportado: " + evento.getAggregateType());
        }
    }

    private void sincronizarLibro(Libro libro) throws Exception {
        String sql = """
            INSERT INTO libros (id, codigo, isbn, titulo, autor, editorial, categoria, anio_publicacion, cantidad_total, disponibles, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            codigo=excluded.codigo, isbn=excluded.isbn, titulo=excluded.titulo, autor=excluded.autor,
            editorial=excluded.editorial, categoria=excluded.categoria, anio_publicacion=excluded.anio_publicacion,
            cantidad_total=excluded.cantidad_total, disponibles=excluded.disponibles, activo=excluded.activo
        """;
        List<Object> args = List.of(
                val(libro.getId()), val(libro.getCodigo()), val(libro.getIsbn()), val(libro.getTitulo()),
                val(libro.getAutor()), val(libro.getEditorial()), val(libro.getCategoria()),
                val(libro.getAnio()), val(libro.getCantidadTotal()), val(libro.getDisponibles()),
                val(libro.getActivo() != null && libro.getActivo() ? 1 : 0)
        );
        ejecutarSqlEnTurso(sql, args);
    }

    private void sincronizarProfesor(Profesor profesor) throws Exception {
        String sql = """
            INSERT INTO profesores (id, dni, nombre, apellido, email_telefono, activo)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            dni=excluded.dni, nombre=excluded.nombre, apellido=excluded.apellido,
            email_telefono=excluded.email_telefono, activo=excluded.activo
        """;
        List<Object> args = List.of(
                val(profesor.getId()), val(profesor.getDni()), val(profesor.getNombre()),
                val(profesor.getApellido()), val(profesor.getEmailTelefono()),
                val(profesor.getActivo() != null && profesor.getActivo() ? 1 : 0)
        );
        ejecutarSqlEnTurso(sql, args);
    }

    private void sincronizarUsuario(Usuario usuario) throws Exception {
        String sql = """
            INSERT INTO usuarios (id, username, password_hash, nombre, rol, activo)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            username=excluded.username, password_hash=excluded.password_hash,
            nombre=excluded.nombre, rol=excluded.rol, activo=excluded.activo
        """;
        List<Object> args = List.of(
                val(usuario.getId()), val(usuario.getUsername()), val(usuario.getPasswordHash()),
                val(usuario.getNombre()), val(usuario.getRol()),
                val(usuario.getActivo() != null && usuario.getActivo() ? 1 : 0)
        );
        ejecutarSqlEnTurso(sql, args);
    }

    private void sincronizarPrestamo(Prestamo prestamo) throws Exception {
        String sqlPrestamo = """
            INSERT INTO prestamos (id, profesor_id, usuario_id, fecha_vencimiento, fecha_devolucion, materia, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            fecha_devolucion=excluded.fecha_devolucion, estado=excluded.estado
        """;
        List<Object> argsPrestamo = List.of(
                val(prestamo.getId()),
                val(prestamo.getProfesor() != null ? prestamo.getProfesor().getId() : null),
                val(prestamo.getUsuarioAlta() != null ? prestamo.getUsuarioAlta().getId() : null),
                val(prestamo.getFechaVencimiento() != null ? prestamo.getFechaVencimiento().toString() : null),
                val(prestamo.getFechaDevolucion() != null ? prestamo.getFechaDevolucion().toString() : null),
                val(prestamo.getMateria()),
                val(prestamo.getEstado())
        );
        ejecutarSqlEnTurso(sqlPrestamo, argsPrestamo);

        if (prestamo.getDetalles() != null) {
            String sqlDetalle = """
                INSERT INTO prestamo_detalle (id, prestamo_id, libro_id, cantidad, cantidad_devuelta)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                cantidad_devuelta=excluded.cantidad_devuelta
            """;
            for (PrestamoDetalle detalle : prestamo.getDetalles()) {
                List<Object> argsDetalle = List.of(
                        val(detalle.getId()), val(prestamo.getId()),
                        val(detalle.getLibro() != null ? detalle.getLibro().getId() : null),
                        val(detalle.getCantidad()), val(detalle.getCantidadDevuelta())
                );
                ejecutarSqlEnTurso(sqlDetalle, argsDetalle);
            }
        }
    }

    private void sincronizarAuditoria(Auditoria auditoria) throws Exception {
        String sql = """
            INSERT INTO auditoria (id, usuario_id, accion, entidad, entidad_id, detalle)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO NOTHING
        """;
        List<Object> args = List.of(
                val(auditoria.getId()),
                val(auditoria.getUsuario() != null ? auditoria.getUsuario().getId() : null),
                val(auditoria.getAccion()), val(auditoria.getEntidad()),
                val(auditoria.getEntidadId()), val(auditoria.getDetalle())
        );
        ejecutarSqlEnTurso(sql, args);
    }

    private Object val(Object o) {
        return o != null ? o : "null_value_marker";
    }

    /**
     * Envía la consulta SQL formateada en JSON a la API REST Hrana v2 de Turso.
     */
    private void ejecutarSqlEnTurso(String sql, List<Object> args) throws Exception {
        String hostLimpio = tursoUrl.replaceAll("^(jdbc:)?(libsql|https?)://", "").replaceAll("/.*$", "").trim();
        String endpoint = "https://" + hostLimpio + "/v2/pipeline";

        Map<String, Object> stmtMap = new LinkedHashMap<>();
        stmtMap.put("sql", sql);

        if (!args.isEmpty()) {
            List<Map<String, Object>> formattedArgs = new ArrayList<>();
            for (Object arg : args) {
                Map<String, Object> argMap = new LinkedHashMap<>();
                if ("null_value_marker".equals(arg)) {
                    argMap.put("type", "null");
                } else if (arg instanceof Integer || arg instanceof Long) {
                    argMap.put("type", "integer");
                    argMap.put("value", String.valueOf(arg));
                } else {
                    argMap.put("type", "text");
                    argMap.put("value", String.valueOf(arg));
                }
                formattedArgs.add(argMap);
            }
            stmtMap.put("args", formattedArgs);
        }

        Map<String, Object> executeReq = Map.of("type", "execute", "stmt", stmtMap);
        Map<String, Object> closeReq = Map.of("type", "close");
        Map<String, Object> payload = Map.of("requests", List.of(executeReq, closeReq));

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tursoToken.trim())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error HTTP " + response.statusCode() + " en Turso: " + response.body());
        }

        if (response.body().contains("\"type\":\"error\"")) {
            throw new RuntimeException("Error SQL en Turso: " + response.body());
        }
    }
}