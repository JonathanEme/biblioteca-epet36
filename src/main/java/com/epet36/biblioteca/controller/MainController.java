package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.config.SesionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private Button btnUsuarios; // Botón inyectado

    private final ApplicationContext applicationContext;
    private final SesionManager sesionManager; // Inyectamos la sesión

    @FXML
    public void initialize() {
        // VALIDACIÓN: Si no es ADMIN, ocultamos el botón de Usuarios
        if (!sesionManager.isAdmin()) {
            btnUsuarios.setVisible(false);
            btnUsuarios.setManaged(false); // Quita el espacio en blanco
        }

        mostrarDashboard();
    }

    @FXML
    public void cerrarSesion() {
        // Limpiamos la sesión en memoria por seguridad antes de salir
        sesionManager.cerrarSesion();

        // Cerramos la interfaz gráfica de JavaFX
        javafx.application.Platform.exit();

        // Apagamos la máquina virtual de Java (mata el proceso de Spring Boot)
        System.exit(0);
    }

    @FXML public void mostrarDashboard() { cargarVista("/view/dashboard.fxml"); }
    @FXML public void mostrarNuevoPrestamo() { cargarVista("/view/nuevo_prestamo.fxml"); }
    @FXML public void mostrarDevolucion() { cargarVista("/view/devolucion.fxml"); }
    @FXML public void mostrarProfesores() { cargarVista("/view/profesores.fxml"); }
    @FXML public void mostrarInventario() { cargarVista("/view/inventario.fxml"); }
    @FXML public void mostrarUsuarios() { cargarVista("/view/usuarios.fxml"); }

    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent vista = loader.load();
            rootPane.setCenter(vista);
        } catch (IOException e) {
            log.error("Error al cargar la vista FXML: {}", fxmlPath, e);
        }
    }
}