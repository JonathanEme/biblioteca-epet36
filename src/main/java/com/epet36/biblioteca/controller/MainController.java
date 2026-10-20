package com.epet36.biblioteca.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainController {

    @FXML
    private BorderPane rootPane;

    private final ApplicationContext applicationContext;

    @FXML
    public void initialize() {
        // Al iniciar, cargamos el Dashboard por defecto
        mostrarDashboard();
    }

    @FXML
    public void mostrarDashboard() {
        cargarVista("/view/dashboard.fxml");
    }

    @FXML
    public void mostrarNuevoPrestamo() {
        cargarVista("/view/nuevo_prestamo.fxml");
    }

    @FXML
    public void mostrarDevolucion() {
        cargarVista("/view/devolucion.fxml");
    }

    @FXML
    public void mostrarProfesores() {
        cargarVista("/view/profesores.fxml");
    }

    @FXML
    public void mostrarInventario() {
        cargarVista("/view/inventario.fxml");
    }

    /**
     * Método genérico para cargar cualquier FXML en el centro del BorderPane
     */
    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            // Le indicamos al loader que resuelva los beans de los controladores mediante Spring
            loader.setControllerFactory(applicationContext::getBean);
            Parent vista = loader.load();

            // Reemplazamos el contenido central del BorderPane
            rootPane.setCenter(vista);
        } catch (IOException e) {
            log.error("Error al cargar la vista FXML en la ruta: {}", fxmlPath, e);
        }
    }
}