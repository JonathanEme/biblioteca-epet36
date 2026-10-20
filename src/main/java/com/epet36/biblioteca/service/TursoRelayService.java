package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.*;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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

    @Scheduled(fixedDelay = 30000)
    public void procesarEventosOutbox() {
        if (tursoUrl == null || tursoUrl.isBlank() || tursoToken == null || tursoToken.isBlank()) {
            return;
        }

        List<OutboxEvent> eventosPendientes = outboxRepository.findAllByOrderByCreatedAtAsc();
        if (eventosPendientes.isEmpty()) {
            return;
        }

        String connectionString = "jdbc:libsql://" + tursoUrl + "?authToken=" + tursoToken;

        try (Connection conn = DriverManager.getConnection(connectionString)) {
            conn.setAutoCommit(false);

            // 1. Aseguramos la existencia de las tablas en Turso antes de escribir
            inicializarEsquemaTurso(conn);

            // 2. Procesamos el lote de eventos pendientes
            for (OutboxEvent evento : eventosPendientes) {
                if (evento.getIntentos() >= MAX_INTENTOS) {
                    continue;
                }

                try {
                    despacharEvento(conn, evento);
                    outboxRepository.delete(evento); // Se elimina localmente al confirmar en Turso
                } catch (Exception e) {
                    conn.rollback();
                    log.error("Fallo al procesar evento Outbox ID {}: {}", evento.getId(), e.getMessage());

                    evento.setIntentos(evento.getIntentos() + 1);
                    evento.setUltimoError(e.getMessage());
                    outboxRepository.save(evento);
                    return; // Interrumpe para reintentar en el próximo ciclo manteniendo el orden
                }
            }
            conn.commit();
            log.info("Sincronización Outbox completada con éxito.");

        } catch (SQLException e) {
            log.warn("Turso inalcanzable. Se reintentará en el próximo ciclo. Detalle: {}", e.getMessage());
        }
    }

    private void inicializarEsquemaTurso(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (id TEXT PRIMARY KEY, username TEXT, password_hash TEXT, nombre TEXT, rol TEXT, activo INTEGER);
                CREATE TABLE IF NOT EXISTS profesores (id TEXT PRIMARY KEY, dni TEXT, nombre TEXT, apellido TEXT, email_telefono TEXT, activo INTEGER);
                CREATE TABLE IF NOT EXISTS libros (id TEXT PRIMARY KEY, codigo TEXT, isbn TEXT, titulo TEXT, autor TEXT, editorial TEXT, categoria TEXT, anio_publicacion INTEGER, cantidad_total INTEGER, disponibles INTEGER, activo INTEGER);
                CREATE TABLE IF NOT EXISTS prestamos (id TEXT PRIMARY KEY, profesor_id TEXT, usuario_id TEXT, fecha_vencimiento TEXT, fecha_devolucion TEXT, materia TEXT, estado TEXT);
                CREATE TABLE IF NOT EXISTS prestamo_detalle (id TEXT PRIMARY KEY, prestamo_id TEXT, libro_id TEXT, cantidad INTEGER, cantidad_devuelta INTEGER);
                CREATE TABLE IF NOT EXISTS auditoria (id TEXT PRIMARY KEY, usuario_id TEXT, accion TEXT, entidad TEXT, entidad_id TEXT, detalle TEXT);
            """);
        }
    }

    private void despacharEvento(Connection conn, OutboxEvent evento) throws Exception {
        switch (evento.getAggregateType()) {
            case "Libro" -> sincronizarLibro(conn, objectMapper.readValue(evento.getPayload(), Libro.class));
            case "Profesor" -> sincronizarProfesor(conn, objectMapper.readValue(evento.getPayload(), Profesor.class));
            case "Usuario" -> sincronizarUsuario(conn, objectMapper.readValue(evento.getPayload(), Usuario.class));
            case "Prestamo" -> sincronizarPrestamo(conn, objectMapper.readValue(evento.getPayload(), Prestamo.class));
            case "Auditoria" -> sincronizarAuditoria(conn, objectMapper.readValue(evento.getPayload(), Auditoria.class));
            default -> throw new IllegalArgumentException("Tipo de entidad no soportado: " + evento.getAggregateType());
        }
    }

    private void sincronizarLibro(Connection conn, Libro libro) throws SQLException {
        String sql = """
            INSERT INTO libros (id, codigo, isbn, titulo, autor, editorial, categoria, anio_publicacion, cantidad_total, disponibles, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            codigo=excluded.codigo, isbn=excluded.isbn, titulo=excluded.titulo, autor=excluded.autor,
            editorial=excluded.editorial, categoria=excluded.categoria, anio_publicacion=excluded.anio_publicacion,
            cantidad_total=excluded.cantidad_total, disponibles=excluded.disponibles, activo=excluded.activo
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, libro.getId());
            pstmt.setString(2, libro.getCodigo());
            pstmt.setString(3, libro.getIsbn());
            pstmt.setString(4, libro.getTitulo());
            pstmt.setString(5, libro.getAutor());
            pstmt.setString(6, libro.getEditorial());
            pstmt.setString(7, libro.getCategoria());
            if (libro.getAnio() != null) pstmt.setInt(8, libro.getAnio()); else pstmt.setNull(8, java.sql.Types.INTEGER);
            pstmt.setInt(9, libro.getCantidadTotal() != null ? libro.getCantidadTotal() : 1);
            pstmt.setInt(10, libro.getDisponibles() != null ? libro.getDisponibles() : 1);
            pstmt.setBoolean(11, libro.getActivo() != null ? libro.getActivo() : true);
            pstmt.executeUpdate();
        }
    }

    private void sincronizarProfesor(Connection conn, Profesor profesor) throws SQLException {
        String sql = """
            INSERT INTO profesores (id, dni, nombre, apellido, email_telefono, activo)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            dni=excluded.dni, nombre=excluded.nombre, apellido=excluded.apellido,
            email_telefono=excluded.email_telefono, activo=excluded.activo
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profesor.getId());
            pstmt.setString(2, profesor.getDni());
            pstmt.setString(3, profesor.getNombre());
            pstmt.setString(4, profesor.getApellido());
            pstmt.setString(5, profesor.getEmailTelefono());
            pstmt.setBoolean(6, profesor.getActivo() != null ? profesor.getActivo() : true);
            pstmt.executeUpdate();
        }
    }

    private void sincronizarUsuario(Connection conn, Usuario usuario) throws SQLException {
        String sql = """
            INSERT INTO usuarios (id, username, password_hash, nombre, rol, activo)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            username=excluded.username, password_hash=excluded.password_hash,
            nombre=excluded.nombre, rol=excluded.rol, activo=excluded.activo
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getId());
            pstmt.setString(2, usuario.getUsername());
            pstmt.setString(3, usuario.getPasswordHash());
            pstmt.setString(4, usuario.getNombre());
            pstmt.setString(5, usuario.getRol());
            pstmt.setBoolean(6, usuario.getActivo() != null ? usuario.getActivo() : true);
            pstmt.executeUpdate();
        }
    }

    private void sincronizarPrestamo(Connection conn, Prestamo prestamo) throws SQLException {
        String sqlPrestamo = """
            INSERT INTO prestamos (id, profesor_id, usuario_id, fecha_vencimiento, fecha_devolucion, materia, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            fecha_devolucion=excluded.fecha_devolucion, estado=excluded.estado
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sqlPrestamo)) {
            pstmt.setString(1, prestamo.getId());
            pstmt.setString(2, prestamo.getProfesor() != null ? prestamo.getProfesor().getId() : null);
            pstmt.setString(3, prestamo.getUsuarioAlta() != null ? prestamo.getUsuarioAlta().getId() : null);
            pstmt.setString(4, prestamo.getFechaVencimiento() != null ? prestamo.getFechaVencimiento().toString() : null);
            pstmt.setString(5, prestamo.getFechaDevolucion() != null ? prestamo.getFechaDevolucion().toString() : null);
            pstmt.setString(6, prestamo.getMateria());
            pstmt.setString(7, prestamo.getEstado());
            pstmt.executeUpdate();
        }

        if (prestamo.getDetalles() != null) {
            String sqlDetalle = """
                INSERT INTO prestamo_detalle (id, prestamo_id, libro_id, cantidad, cantidad_devuelta)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                cantidad_devuelta=excluded.cantidad_devuelta
            """;
            for (PrestamoDetalle detalle : prestamo.getDetalles()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDetalle)) {
                    pstmt.setString(1, detalle.getId());
                    pstmt.setString(2, prestamo.getId());
                    pstmt.setString(3, detalle.getLibro() != null ? detalle.getLibro().getId() : null);
                    pstmt.setInt(4, detalle.getCantidad() != null ? detalle.getCantidad() : 1);
                    pstmt.setInt(5, detalle.getCantidadDevuelta() != null ? detalle.getCantidadDevuelta() : 0);
                    pstmt.executeUpdate();
                }
            }
        }
    }

    private void sincronizarAuditoria(Connection conn, Auditoria auditoria) throws SQLException {
        String sql = """
            INSERT INTO auditoria (id, usuario_id, accion, entidad, entidad_id, detalle)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO NOTHING
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auditoria.getId());
            pstmt.setString(2, auditoria.getUsuario() != null ? auditoria.getUsuario().getId() : null);
            pstmt.setString(3, auditoria.getAccion());
            pstmt.setString(4, auditoria.getEntidad());
            pstmt.setString(5, auditoria.getEntidadId());
            pstmt.setString(6, auditoria.getDetalle());
            pstmt.executeUpdate();
        }
    }
}