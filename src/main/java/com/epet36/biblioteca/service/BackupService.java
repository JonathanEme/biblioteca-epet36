package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final AuditoriaService auditoriaService;
    private final JdbcTemplate jdbcTemplate; // Inyectamos JdbcTemplate para comandos SQLite nativos

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private final String BACKUP_FOLDER = "backups";

    private String getDatabaseFilePath() {
        // Limpiamos la URL por si tiene parámetros JDBC
        String path = datasourceUrl.replace("jdbc:sqlite:", "");
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        return path;
    }

    /**
     * Crea un backup seguro usando el comando nativo VACUUM INTO de SQLite.
     * Esto evita copiar el archivo en medio de una transacción y garantiza que no falten datos del WAL.
     */
    public String crearBackup(Usuario usuarioAutorizado) {
        try {
            Path backupDir = Paths.get(BACKUP_FOLDER);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "backup_biblioteca_" + timestamp + ".db";
            Path targetPath = backupDir.resolve(backupFileName).toAbsolutePath();

            // Usamos las barras inclinadas hacia adelante para que SQLite entienda la ruta en Windows
            String targetPathSql = targetPath.toString().replace("\\", "/");

            // Comando NATIVO de SQLite para hacer backup consistente en caliente
            jdbcTemplate.execute("VACUUM INTO '" + targetPathSql + "'");

            auditoriaService.registrar("BACKUP_CREAR", "Sistema", null, usuarioAutorizado,
                    "Backup creado exitosamente: " + backupFileName);

            log.info("Backup creado exitosamente mediante VACUUM INTO: {}", targetPath);
            return targetPath.toString();

        } catch (Exception e) {
            log.error("Error al crear el backup", e);
            throw new RuntimeException("Error al generar el backup de la base de datos.", e);
        }
    }

    /**
     * Restaura un backup específico.
     */
    public void restaurarBackup(String nombreArchivoBackup, Usuario usuarioAutorizado) {
        try {
            Path backupPath = Paths.get(BACKUP_FOLDER).resolve(nombreArchivoBackup);
            if (!Files.exists(backupPath)) {
                throw new IllegalArgumentException("El archivo de backup no existe: " + nombreArchivoBackup);
            }

            // 1. Resguardo previo
            crearBackup(usuarioAutorizado);

            Path dbPath = Paths.get(getDatabaseFilePath()).toAbsolutePath();

            // 2. Forzamos un checkpoint en SQLite para consolidar transacciones
            jdbcTemplate.execute("PRAGMA wal_checkpoint(TRUNCATE);");

            // 3. Copia con reemplazo
            Files.copy(backupPath, dbPath, StandardCopyOption.REPLACE_EXISTING);

            auditoriaService.registrar("BACKUP_RESTAURAR", "Sistema", null, usuarioAutorizado,
                    "Sistema restaurado desde el backup: " + nombreArchivoBackup);

            log.info("Backup restaurado desde: {}", backupPath.toAbsolutePath());

        } catch (FileSystemException e) {
            log.error("Error de bloqueo de archivo al restaurar", e);
            throw new RuntimeException("No se pudo reemplazar la base de datos porque está en uso por la aplicación. Reinicie la aplicación e intente nuevamente.", e);
        } catch (IOException e) {
            log.error("Error al restaurar el backup", e);
            throw new RuntimeException("Error al restaurar la base de datos.", e);
        }
    }

    /**
     * Lista todos los backups disponibles, manejando el caso de carpeta inexistente.
     */
    public List<String> listarBackups() {
        Path backupDir = Paths.get(BACKUP_FOLDER);

        // Si la carpeta no existe todavía, devuelve lista vacía en lugar de lanzar excepción
        if (!Files.exists(backupDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> paths = Files.list(backupDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".db"))
                    .sorted((a, b) -> b.compareTo(a))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error al listar backups", e);
            throw new RuntimeException("No se pudo acceder a la carpeta de backups.", e);
        }
    }
}