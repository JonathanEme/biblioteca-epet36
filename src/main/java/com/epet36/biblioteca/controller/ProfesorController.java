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

    // ID del profesor seleccionado (para saber si estamos editando o creando)
    private Long profesorSeleccionadoId = null;

    private ObservableList<Profesor> profesoresList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarDatos();

        // Listener para cargar los datos en el formulario cuando se hace clic en una fila
        tablaProfesores.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> seleccionarProfesor(newValue)
        );
    }

    private void configurarTabla() {
        colDni.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDni()));
        colApellido.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getApellido()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));

        colContacto.setCellValueFactory(cell -> {
            String contacto = cell.getValue().getEmailTelefono();
            return new SimpleStringProperty(contacto != null ? contacto : "");
        });

        colEstado.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getActivo() ? "Activo" : "Inactivo")
        );

        tablaProfesores.setItems(profesoresList);
    }

    private void cargarDatos() {
        // Obtenemos todos los profesores (activos e inactivos para poder administrarlos)
        // Usamos el repositorio directamente o agregamos un método findAll() en el servicio
        List<Profesor> todos = profesorService.obtenerTodosActivos(); // Por ahora mostramos los activos, luego ajustamos
        profesoresList.clear();
        profesoresList.addAll(todos);
    }

    private void seleccionarProfesor(Profesor profesor) {
        if (profesor != null) {
            profesorSeleccionadoId = profesor.getId();
            txtDni.setText(profesor.getDni());
            txtNombre.setText(profesor.getNombre());
            txtApellido.setText(profesor.getApellido());
            txtContacto.setText(profesor.getEmailTelefono());
            chkActivo.setSelected(profesor.getActivo());
            // Deshabilitamos el DNI para no romper relaciones si se edita
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
                    .id(profesorSeleccionadoId) // Si es null, Hibernate hace INSERT. Si tiene número, hace UPDATE.
                    .dni(txtDni.getText().trim())
                    .nombre(txtNombre.getText().trim())
                    .apellido(txtApellido.getText().trim())
                    .emailTelefono(txtContacto.getText().trim())
                    .activo(chkActivo.isSelected())
                    .build();

            profesorService.guardar(profesor);
            mostrarMensajeExito("Profesor guardado correctamente.");

            onLimpiarClick(); // Limpia formulario
            cargarDatos();    // Refresca tabla

        } catch (Exception e) {
            mostrarMensajeError(e.getMessage());
        }
    }

    @FXML
    public void onLimpiarClick() {
        profesorSeleccionadoId = null;
        txtDni.clear();
        txtDni.setDisable(false); // Volvemos a habilitar para nuevos registros
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