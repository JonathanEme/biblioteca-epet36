package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final AuditoriaService auditoriaService;

    // Tomamos la URL de la base de datos desde el application.yml (ej: jdbc:sqlite:biblioteca.db)
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private final String BACKUP_FOLDER = "backups";

    /**
     * Extrae el nombre del archivo de la URL de conexión JDBC.
     */
    private String getDatabaseFilePath() {
        return datasourceUrl.replace("jdbc:sqlite:", "");
    }

    /**
     * Crea un backup de la base de datos actual.
     */
    public String crearBackup(Usuario usuarioAutorizado) {
        try {
            Path dbPath = Paths.get(getDatabaseFilePath());
            if (!Files.exists(dbPath)) {
                throw new IllegalStateException("El archivo de base de datos no existe: " + dbPath);
            }

            Path backupDir = Paths.get(BACKUP_FOLDER);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "backup_biblioteca_" + timestamp + ".db";
            Path targetPath = backupDir.resolve(backupFileName);

            // Copiamos el archivo. REPLACE_EXISTING por si acaso, aunque el timestamp lo hace único.
            Files.copy(dbPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            auditoriaService.registrar("BACKUP_CREAR", "Sistema", null, usuarioAutorizado,
                    "Backup creado exitosamente: " + backupFileName);

            log.info("Backup creado: {}", targetPath.toAbsolutePath());
            return targetPath.toString();

        } catch (IOException e) {
            log.error("Error al crear backup", e);
            throw new RuntimeException("Error al crear el backup de la base de datos.", e);
        }
    }

    /**
     * Restaura un backup específico. Por regla de negocio, primero hace un backup del estado actual.
     */
    public void restaurarBackup(String nombreArchivoBackup, Usuario usuarioAutorizado) {
        try {
            Path backupPath = Paths.get(BACKUP_FOLDER).resolve(nombreArchivoBackup);
            if (!Files.exists(backupPath)) {
                throw new IllegalArgumentException("El archivo de backup no existe: " + nombreArchivoBackup);
            }

            // Regla de Negocio Crítica: Backup de seguridad antes de pisar los datos
            crearBackup(usuarioAutorizado);

            Path dbPath = Paths.get(getDatabaseFilePath());

            // ATENCIÓN: En un entorno de producción estricto con SQLite, idealmente se debe asegurar
            // que no hay transacciones escribiendo en este milisegundo.
            Files.copy(backupPath, dbPath, StandardCopyOption.REPLACE_EXISTING);

            auditoriaService.registrar("BACKUP_RESTAURAR", "Sistema", null, usuarioAutorizado,
                    "Sistema restaurado desde el backup: " + nombreArchivoBackup);

            log.info("Backup restaurado desde: {}", backupPath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Error al restaurar backup", e);
            throw new RuntimeException("Error al restaurar la base de datos.", e);
        }
    }

    /**
     * Lista todos los backups disponibles en la carpeta.
     */
    public List<String> listarBackups() {
        try (Stream<Path> paths = Files.list(Paths.get(BACKUP_FOLDER))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".db"))
                    .sorted((a, b) -> b.compareTo(a)) // Orden descendente (más recientes primero)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error al listar backups", e);
            throw new RuntimeException("No se pudo acceder a la carpeta de backups.", e);
        }
    }
}