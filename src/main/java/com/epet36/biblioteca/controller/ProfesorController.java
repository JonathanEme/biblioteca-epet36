package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.service.ProfesorService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfesorController {

    private final ProfesorService profesorService;

    // Elementos de la Tabla
    @FXML private TableView<Profesor> tablaProfesores;
    @FXML private TableColumn<Profesor, String> colDni;
    @FXML private TableColumn<Profesor, String> colApellido;
    @FXML private TableColumn<Profesor, String> colNombre;
    @FXML private TableColumn<Profesor, String> colContacto;
    @FXML private TableColumn<Profesor, String> colEstado;

    // Elementos del Formulario
    @FXML private TextField txtDni;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtContacto;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblMensaje;

    // CAMBIO VITAL: Ahora la referencia es String (UUID)
    private String profesorSeleccionadoId = null;

    private final ObservableList<Profesor> profesoresList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarDatos();

        // Listener para cargar los datos en el formulario cuando se selecciona una fila
        tablaProfesores.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> seleccionarProfesor(newValue)
        );
    }

    private void configurarTabla() {
        // Blindaje contra valores nulos en la tabla
        colDni.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDni() != null ? cell.getValue().getDni() : ""));

        colApellido.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getApellido() != null ? cell.getValue().getApellido() : ""));

        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre() != null ? cell.getValue().getNombre() : ""));

        colContacto.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getEmailTelefono() != null ? cell.getValue().getEmailTelefono() : ""));

        colEstado.setCellValueFactory(cell ->
                new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getActivo()) ? "Activo" : "Inactivo")
        );

        tablaProfesores.setItems(profesoresList);
    }

    private void cargarDatos() {
        List<Profesor> todos = profesorService.obtenerTodosActivos();
        profesoresList.clear();
        profesoresList.addAll(todos);
    }

    private void seleccionarProfesor(Profesor profesor) {
        if (profesor != null) {
            profesorSeleccionadoId = profesor.getId(); // String UUID
            txtDni.setText(profesor.getDni() != null ? profesor.getDni() : "");
            txtNombre.setText(profesor.getNombre());
            txtApellido.setText(profesor.getApellido());
            txtContacto.setText(profesor.getEmailTelefono() != null ? profesor.getEmailTelefono() : "");
            chkActivo.setSelected(Boolean.TRUE.equals(profesor.getActivo()));

            // Deshabilitamos el DNI al editar para mantener la integridad
            txtDni.setDisable(true);
        }
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank()) {
                mostrarMensajeError("Nombre y Apellido son obligatorios.");
                return;
            }

            Profesor profesor = Profesor.builder()
                    .id(profesorSeleccionadoId) // Si es null, ProfesorService asignará un nuevo UUID
                    .dni(txtDni.getText().trim())
                    .nombre(txtNombre.getText().trim())
                    .apellido(txtApellido.getText().trim())
                    .emailTelefono(txtContacto.getText().trim())
                    .activo(chkActivo.isSelected())
                    .build();

            // Llama al servicio que guarda en SQLite y registra el evento en outbox_events
            profesorService.guardar(profesor);
            mostrarMensajeExito("Profesor guardado correctamente.");

            onLimpiarClick();
            cargarDatos();

        } catch (Exception e) {
            mostrarMensajeError(e.getMessage());
        }
    }

    @FXML
    public void onLimpiarClick() {
        profesorSeleccionadoId = null;
        txtDni.clear();
        txtDni.setDisable(false);
        txtNombre.clear();
        txtApellido.clear();
        txtContacto.clear();
        chkActivo.setSelected(true);
        tablaProfesores.getSelectionModel().clearSelection();
        lblMensaje.setText("");
    }

    private void mostrarMensajeExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #155724;");
    }

    private void mostrarMensajeError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #721c24;");
    }
}