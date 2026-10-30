package com.epet36.biblioteca.service;

import com.epet36.biblioteca.event.DataSincronizadaEvent;
import com.epet36.biblioteca.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TursoInboundService {

    private final LibroService libroService;
    private final ProfesorService profesorService;
    private final UsuarioService usuarioService;
    private final PrestamoService prestamoService;

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${turso.url:}")
    private String tursoUrl;

    @Value("${turso.token:}")
    private String tursoToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Scheduled(fixedDelay = 120000) // Cada 2 minutos
    public void sincronizarDatosDesdeTurso() {
        if (tursoUrl == null || tursoUrl.isBlank() || tursoToken == null || tursoToken.isBlank()) {
            return;
        }

        log.info("Iniciando sincronización de entrada (Pull) desde Turso...");
        try {
            // 1. DESCARGAMOS Y MAPEAMOS LAS ENTIDADES PADRES
            sincronizarUsuarios();
            sincronizarProfesores();
            sincronizarLibros();

            // 2. DESCARGAMOS Y MAPEAMOS LAS ENTIDADES HIJAS (Dependen de que los padres ya existan)
            sincronizarPrestamos();

            log.info("Sincronización de entrada completada con éxito.");

            // 3. PUBLICAMOS EL EVENTO PARA NOTIFICAR Y REFRESCAR LA INTERFAZ DE JAVAFX
            eventPublisher.publishEvent(new DataSincronizadaEvent(this));

        } catch (Exception e) {
            log.error("Error crítico al traer datos desde Turso: ", e);
        }
    }

    // =========================================================================
    // LÓGICA DE MAPEO DE ENTIDADES
    // =========================================================================

    private void sincronizarUsuarios() throws Exception {
        JsonNode result = ejecutarSelectEnTurso("SELECT * FROM usuarios");
        JsonNode cols = result.path("cols");

        for (JsonNode row : result.path("rows")) {
            Usuario u = new Usuario();
            u.setId(getColValue(cols, row, "id"));
            u.setUsername(getColValue(cols, row, "username"));
            u.setPasswordHash(getColValue(cols, row, "password_hash"));
            u.setNombre(getColValue(cols, row, "nombre"));
            u.setRol(getColValue(cols, row, "rol"));
            u.setActivo(getBooleanValue(cols, row, "activo"));

            usuarioService.sincronizarDesdeNube(u);
        }
    }

    private void sincronizarProfesores() throws Exception {
        JsonNode result = ejecutarSelectEnTurso("SELECT * FROM profesores");
        JsonNode cols = result.path("cols");

        for (JsonNode row : result.path("rows")) {
            Profesor p = new Profesor();
            p.setId(getColValue(cols, row, "id"));
            p.setDni(getColValue(cols, row, "dni"));
            p.setNombre(getColValue(cols, row, "nombre"));
            p.setApellido(getColValue(cols, row, "apellido"));
            p.setEmailTelefono(getColValue(cols, row, "email_telefono"));
            p.setActivo(getBooleanValue(cols, row, "activo"));

            profesorService.sincronizarDesdeNube(p);
        }
    }

    private void sincronizarLibros() throws Exception {
        JsonNode result = ejecutarSelectEnTurso("SELECT * FROM libros");
        JsonNode cols = result.path("cols");

        for (JsonNode row : result.path("rows")) {
            Libro l = Libro.builder()
                    .id(getColValue(cols, row, "id"))
                    .codigo(getColValue(cols, row, "codigo"))
                    .isbn(getColValue(cols, row, "isbn"))
                    .titulo(getColValue(cols, row, "titulo"))
                    .autor(getColValue(cols, row, "autor"))
                    .editorial(getColValue(cols, row, "editorial"))
                    .categoria(getColValue(cols, row, "categoria"))
                    .anio(getIntValue(cols, row, "anio_publicacion"))
                    .cantidadTotal(getIntValue(cols, row, "cantidad_total") != null ? getIntValue(cols, row, "cantidad_total") : 1)
                    .disponibles(getIntValue(cols, row, "disponibles") != null ? getIntValue(cols, row, "disponibles") : 1)
                    .activo(getBooleanValue(cols, row, "activo"))
                    .build();

            libroService.sincronizarDesdeNube(l);
        }
    }

    private void sincronizarPrestamos() throws Exception {
        // MAGIA SQL: Filtramos los activos/parciales, y las devoluciones de la última semana
        String sql = "SELECT * FROM prestamos WHERE estado IN ('ACTIVO', 'PARCIAL') OR fecha_devolucion >= date('now', '-7 days')";

        JsonNode resultPrestamos = ejecutarSelectEnTurso(sql);
        JsonNode colsPrestamos = resultPrestamos.path("cols");

        for (JsonNode row : resultPrestamos.path("rows")) {
            Prestamo p = new Prestamo();
            p.setId(getColValue(colsPrestamos, row, "id"));

            // 1. Setear el Profesor
            String profId = getColValue(colsPrestamos, row, "profesor_id");
            if (profId != null) {
                p.setProfesor(profesorService.buscarPorIdOpcional(profId).orElse(null));
            }

            // 2. Setear el Usuario Alta de forma segura
            String usuarioId = getColValue(colsPrestamos, row, "usuario_id");
            Usuario usuarioLocal = null;

            if (usuarioId != null) {
                usuarioLocal = usuarioService.buscarPorIdOpcional(usuarioId).orElse(null);
            }

            // EL SALVAVIDAS: Si el usuario que creó el préstamo en la nube no existe en esta compu,
            // se lo asignamos al 'admin' local para que Hibernate no rompa la sincronización.
            if (usuarioLocal == null) {
                log.warn("El usuario original del préstamo ID {} no existe localmente. Asignando al admin por defecto.", p.getId());
                // Buscamos al admin usando el UUID fijo que configuramos en el DataInitializer
                usuarioLocal = usuarioService.buscarPorIdOpcional("admin-user-0000-0000-000000000000").orElse(null);

                // Si por alguna rarísima razón el admin tampoco está por ID, lo buscamos por nombre
                // (Para asegurar 100% que nunca sea null)
                if (usuarioLocal == null) {
                    usuarioLocal = usuarioService.obtenerUsuarioPorDefecto(); // Asegurate de que este método exista en tu servicio y busque por username "admin"
                }
            }

            p.setUsuarioAlta(usuarioLocal);

            // fechaVencimiento es LocalDate
            String fechaV = getColValue(colsPrestamos, row, "fecha_vencimiento");
            p.setFechaVencimiento(parsearLocalDate(fechaV));

            // fechaDevolucion es LocalDateTime
            String fechaD = getColValue(colsPrestamos, row, "fecha_devolucion");
            p.setFechaDevolucion(parsearLocalDateTime(fechaD));

            p.setMateria(getColValue(colsPrestamos, row, "materia"));
            p.setEstado(getColValue(colsPrestamos, row, "estado"));

            // Obtenemos los detalles de este préstamo
            JsonNode resultDetalles = ejecutarSelectEnTurso("SELECT * FROM prestamo_detalle WHERE prestamo_id = '" + p.getId() + "'");
            JsonNode colsDetalles = resultDetalles.path("cols");
            List<PrestamoDetalle> detalles = new ArrayList<>();

            for (JsonNode rowDetalle : resultDetalles.path("rows")) {
                PrestamoDetalle pd = new PrestamoDetalle();
                pd.setId(getColValue(colsDetalles, rowDetalle, "id"));
                pd.setCantidad(getIntValue(colsDetalles, rowDetalle, "cantidad"));
                pd.setCantidadDevuelta(getIntValue(colsDetalles, rowDetalle, "cantidad_devuelta"));

                String libroId = getColValue(colsDetalles, rowDetalle, "libro_id");
                if (libroId != null) {
                    pd.setLibro(libroService.buscarPorIdOpcional(libroId).orElse(null));
                }

                // --- LA LÍNEA MÁGICA: VINCULAMOS EL DETALLE CON SU PADRE ---
                pd.setPrestamo(p);

                detalles.add(pd);
            }
            p.setDetalles(detalles);

            prestamoService.sincronizarDesdeNube(p);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE PARSEO DE FECHAS (LocalDate vs LocalDateTime)
    // =========================================================================

    private LocalDate parsearLocalDate(String fechaStr) {
        if (fechaStr == null || fechaStr.equals("null") || fechaStr.isBlank()) {
            return null;
        }
        try {
            if (fechaStr.contains("T")) {
                return LocalDateTime.parse(fechaStr).toLocalDate();
            } else {
                return LocalDate.parse(fechaStr);
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear LocalDate desde Turso: {}", fechaStr);
            return null;
        }
    }

    private LocalDateTime parsearLocalDateTime(String fechaStr) {
        if (fechaStr == null || fechaStr.equals("null") || fechaStr.isBlank()) {
            return null;
        }
        try {
            if (fechaStr.contains("T")) {
                return LocalDateTime.parse(fechaStr);
            } else {
                return LocalDate.parse(fechaStr).atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear LocalDateTime desde Turso: {}", fechaStr);
            return null;
        }
    }

    // =========================================================================
    // COMUNICACIÓN HTTP Y EXTRACCIÓN DE DATOS DE LA API HRANA V2
    // =========================================================================

    private JsonNode ejecutarSelectEnTurso(String sql) throws Exception {
        String hostLimpio = tursoUrl.replaceAll("^(jdbc:)?(libsql|https?)://", "").replaceAll("/.*$", "").trim();
        String endpoint = "https://" + hostLimpio + "/v2/pipeline";

        Map<String, Object> stmtMap = Map.of("sql", sql);
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

        JsonNode root = objectMapper.readTree(response.body());

        // La API Hrana v2 devuelve un array 'results'. El índice 0 es nuestra petición 'execute'.
        JsonNode executeResult = root.path("results").get(0);
        if (executeResult.path("type").asText().equals("error")) {
            throw new RuntimeException("Error SQL en Turso: " + executeResult.path("error").path("message").asText());
        }

        // --- CORRECCIÓN VITAL ACÁ ---
        // La API envuelve los datos adentro de un nodo "response"
        return executeResult.path("response").path("result");
    }

    // Funciones auxiliares para interpretar la estructura de respuesta de Turso (Hrana)
    private String getColValue(JsonNode cols, JsonNode row, String colName) {
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).path("name").asText().equals(colName)) {
                JsonNode cell = row.get(i);
                if (cell.path("type").asText().equals("null")) return null;
                return cell.path("value").asText();
            }
        }
        return null;
    }

    private Integer getIntValue(JsonNode cols, JsonNode row, String colName) {
        String val = getColValue(cols, row, colName);
        return val != null ? Integer.parseInt(val) : null;
    }

    private Boolean getBooleanValue(JsonNode cols, JsonNode row, String colName) {
        String val = getColValue(cols, row, colName);
        if (val == null) return false;
        // SQLite guarda los booleanos como 1 o 0
        return val.equals("1") || val.equalsIgnoreCase("true");
    }
}