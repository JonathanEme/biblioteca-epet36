package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.service.UsuarioService;
import com.epet36.biblioteca.event.DataSincronizadaEvent;
import org.springframework.context.event.EventListener;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // UI Tabla
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colUsername;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;

    // UI Formulario
    @FXML private TextField txtUsername;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblMensaje;
    @FXML private Label lblPasswordHelper;

    private Usuario usuarioSeleccionado = null;

    @FXML
    public void initialize() {
        // 1. Configurar columnas
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        colRol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRol()));
        colEstado.setCellValueFactory(cell -> {
            Boolean activo = cell.getValue().getActivo();
            return new SimpleStringProperty((activo != null && activo) ? "Activo" : "Inactivo");
        });

        // 2. Llenar combo de roles
        cmbRol.setItems(FXCollections.observableArrayList("ADMIN", "BIBLIOTECARIO"));

        // 3. Listener de selección en la tabla
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                cargarUsuarioEnFormulario(newSel);
            }
        });

        // 4. Cargar datos iniciales
        cargarTabla();
    }

    private void cargarTabla() {
        // Usamos el método que vamos a agregar al servicio
        List<Usuario> usuarios = usuarioService.obtenerTodos();
        tablaUsuarios.setItems(FXCollections.observableArrayList(usuarios));
    }

    private void cargarUsuarioEnFormulario(Usuario usuario) {
        this.usuarioSeleccionado = usuario;
        txtUsername.setText(usuario.getUsername());
        txtNombre.setText(usuario.getNombre());
        cmbRol.setValue(usuario.getRol());
        chkActivo.setSelected(usuario.getActivo() != null && usuario.getActivo());

        // --- ESTA ES LA LÍNEA CRÍTICA QUE MUESTRA LA CONTRASEÑA EN TEXTO PLANO ---
        txtPassword.setText(usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "");

        lblPasswordHelper.setText("Contraseña actual (Modifique para cambiar):");
        lblMensaje.setText("");
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (txtUsername.getText().isBlank() || txtNombre.getText().isBlank() || cmbRol.getValue() == null) {
                mostrarMensajeError("Por favor, complete los campos obligatorios (*).");
                return;
            }

            boolean esNuevo = (usuarioSeleccionado == null);

            if (esNuevo && txtPassword.getText().isBlank()) {
                mostrarMensajeError("La contraseña es obligatoria para usuarios nuevos.");
                return;
            }

            Usuario usuarioAGuardar;

            if (esNuevo) {
                usuarioAGuardar = Usuario.builder()
                        .username(txtUsername.getText().trim())
                        .nombre(txtNombre.getText().trim())
                        .rol(cmbRol.getValue())
                        .activo(chkActivo.isSelected())
                        .passwordHash(txtPassword.getText()) // El servicio lo encriptará
                        .build();
            } else {
                usuarioAGuardar = usuarioSeleccionado;
                usuarioAGuardar.setUsername(txtUsername.getText().trim());
                usuarioAGuardar.setNombre(txtNombre.getText().trim());
                usuarioAGuardar.setRol(cmbRol.getValue());
                usuarioAGuardar.setActivo(chkActivo.isSelected());

                // CAMBIO DE CONTRASEÑA: Si el admin escribió algo nuevo en el campo
                if (!txtPassword.getText().isBlank()) {
                    usuarioAGuardar.setPasswordHash(txtPassword.getText());
                }
            }

            // El UsuarioService (ya configurado en pasos anteriores) va a detectar
            // si la contraseña no empieza con "$2" (BCrypt) y la hasheará automáticamente.
            usuarioService.guardar(usuarioAGuardar);

            mostrarMensajeExito(esNuevo ? "Usuario creado correctamente." : "Usuario actualizado.");
            cargarTabla();
            onLimpiarClick();

        } catch (IllegalStateException e) {
            mostrarMensajeError(e.getMessage());
        } catch (Exception e) {
            mostrarMensajeError("Error inesperado: " + e.getMessage());
        }
    }

    @FXML
    public void onLimpiarClick() {
        usuarioSeleccionado = null;
        tablaUsuarios.getSelectionModel().clearSelection();
        txtUsername.clear();
        txtNombre.clear();
        txtPassword.clear();
        cmbRol.setValue(null);
        chkActivo.setSelected(true);
        lblPasswordHelper.setText("Contraseña (*):");
        lblMensaje.setText("");
    }

    private void mostrarMensajeExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #155724; -fx-background-color: #d4edda; -fx-padding: 5px; -fx-background-radius: 3px;");
    }

    private void mostrarMensajeError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #721c24; -fx-background-color: #f8d7da; -fx-padding: 5px; -fx-background-radius: 3px;");
    }

    @EventListener
    public void onDataSincronizada(DataSincronizadaEvent event) {

    }

    private void cargarDatos() {
    }
}