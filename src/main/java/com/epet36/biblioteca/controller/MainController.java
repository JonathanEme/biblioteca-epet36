package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.config.SesionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox; // <--- Importamos ComboBox
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

    @FXML private BorderPane rootPane;
    @FXML private Button btnUsuarios;
    @FXML private ComboBox<String> cmbTamanioFuente; // <--- Inyectamos el ComboBox desplegable

    private final ApplicationContext applicationContext;
    private final SesionManager sesionManager;

    @FXML
    public void initialize() {
        if (!sesionManager.isAdmin()) {
            btnUsuarios.setVisible(false);
            btnUsuarios.setManaged(false);
        }

        // 1. Cargamos las opciones disponibles en el desplegable
        cmbTamanioFuente.getItems().addAll("12 px", "14 px", "16 px");

        // Seleccionamos por defecto el que coincida con la sesión actual (o 14 px por defecto)
        String tamanioActualStr = (int) SesionManager.getGlobalFontSize() + " px";
        cmbTamanioFuente.setValue(tamanioActualStr);

        // 2. Escuchamos cuando el usuario elige una opción del desplegable
        cmbTamanioFuente.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    // Extraemos solo el número del texto (ej: "16 px" -> 16.0)
                    String numeroStr = newValue.replace(" px", "").trim();
                    double nuevoValor = Double.parseDouble(numeroStr);

                    // Guardamos en la sesión
                    SesionManager.setGlobalFontSize(nuevoValor);

                    if (rootPane != null) {
                        // 1. Aplicamos el estilo al contenedor principal
                        rootPane.setStyle("-fx-font-size: " + nuevoValor + "px;");

                        // 2. Aplicamos el estilo a la pantalla activa en el centro
                        if (rootPane.getCenter() != null) {
                            rootPane.getCenter().setStyle("-fx-font-size: " + nuevoValor + "px;");
                            if (rootPane.getCenter() instanceof Parent) {
                                // Forzamos a la vista central a recalcular su distribución
                                ((Parent) rootPane.getCenter()).requestLayout();
                            }
                        }

                        // 3. Forzamos un redibujo y recálculo completo de toda la ventana
                        rootPane.requestLayout();
                        rootPane.applyCss();
                        rootPane.layout();
                    }
                } catch (NumberFormatException e) {
                    log.error("Error al parsear el tamaño de fuente seleccionada", e);
                }
            }
        });
        mostrarDashboard();

        // Aplicar tamaño inicial al arrancar
        if (rootPane != null) {
            rootPane.setStyle("-fx-font-size: " + SesionManager.getGlobalFontSize() + "px;");
        }
    }

    @FXML
    public void cerrarSesion() {
        sesionManager.cerrarSesion();
        javafx.application.Platform.exit();
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

            // Asegura que cualquier vista que cargues (incluyendo Inventario) tome el tamaño actual
            vista.setStyle("-fx-font-size: " + SesionManager.getGlobalFontSize() + "px;");

            rootPane.setCenter(vista);
        } catch (IOException e) {
            log.error("Error al cargar la vista FXML: {}", fxmlPath, e);
        }
    }
}