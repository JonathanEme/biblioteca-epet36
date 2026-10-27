package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.config.SesionManager;
import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.service.UsuarioService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final SesionManager sesionManager;
    private final ApplicationContext applicationContext;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;

    @FXML
    public void onIngresarClick() {
        try {
            String user = txtUsername.getText().trim();
            String pass = txtPassword.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                mostrarError("Complete todos los campos.");
                return;
            }

            // Autentica usando BCrypt en tu UsuarioService
            Usuario usuarioAutenticado = usuarioService.autenticar(user, pass);

            // Inicia la sesión en memoria
            sesionManager.iniciarSesion(usuarioAutenticado);

            // Cambia a la pantalla principal
            cargarPantallaPrincipal();

        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    public void onSalirClick() {
        Platform.exit();
        System.exit(0);
    }

    private void cargarPantallaPrincipal() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            stage.setScene(new Scene(root, 1024, 768));
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.setResizable(true); // Permitimos maximizar de nuevo
        } catch (IOException e) {
            mostrarError("Error crítico al cargar el sistema principal.");
            log.error("Error al cargar main.fxml", e);
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }
}