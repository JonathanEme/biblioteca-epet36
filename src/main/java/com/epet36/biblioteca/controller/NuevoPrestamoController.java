package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.config.SesionManager;
import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.service.LibroService;
import com.epet36.biblioteca.service.PrestamoService;
import com.epet36.biblioteca.service.ProfesorService;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NuevoPrestamoController {

    private final PrestamoService prestamoService;
    private final ProfesorService profesorService;
    private final LibroService libroService;
    private final SesionManager sesionManager; // Inyectamos la sesión real del usuario logueado

    // UI Profesor
    @FXML private TextField txtProfesorSeleccionado;
    private Profesor profesorSeleccionado = null;

    // UI Libro
    @FXML private TextField txtCodigoLibro;
    @FXML private TextField txtTituloLibro;
    @FXML private TextField txtAutorLibro;
    private Libro libroSeleccionado = null;

    // UI Préstamo
    @FXML private TextField txtMateria;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private Label lblMensaje;

    // Componente para sugerencias de libros
    private ContextMenu sugerenciasPopup;

    @FXML
    public void initialize() {
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
        spnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));

        // 1. RESTRICCIÓN DE FECHAS: Bloquear fechas anteriores a la actual en el DatePicker
        dpFechaVencimiento.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #e0e0e0;"); // Color gris para días deshabilitados
                }
            }
        });

        configurarAutocompletadoLibro();
    }

    // =================================================================================
    // 1. LÓGICA DE AUTOCOMPLETADO DE LIBROS
    // =================================================================================
    private void configurarAutocompletadoLibro() {
        sugerenciasPopup = new ContextMenu();

        txtTituloLibro.textProperty().addListener((observable, oldValue, newValue) -> {
            if (txtTituloLibro.isFocused() && newValue != null && newValue.length() >= 3) {
                mostrarSugerencias(newValue, txtTituloLibro);
            } else if (newValue == null || newValue.length() < 3) {
                sugerenciasPopup.hide();
            }
        });

        txtCodigoLibro.textProperty().addListener((observable, oldValue, newValue) -> {
            if (txtCodigoLibro.isFocused() && newValue != null && newValue.length() >= 3) {
                mostrarSugerencias(newValue, txtCodigoLibro);
            } else if (newValue == null || newValue.length() < 3) {
                sugerenciasPopup.hide();
            }
        });
    }

    private void mostrarSugerencias(String filtro, TextField campoAsociado) {
        List<Libro> sugerencias = libroService.buscarSugerencias(filtro);
        sugerenciasPopup.getItems().clear();

        if (sugerencias.isEmpty()) {
            sugerenciasPopup.hide();
            return;
        }

        for (Libro libro : sugerencias) {
            String codigoTexto = (libro.getCodigo() != null && !libro.getCodigo().isBlank())
                    ? libro.getCodigo()
                    : (libro.getIsbn() != null ? libro.getIsbn() : "");

            MenuItem item = new MenuItem(libro.getTitulo() + " - " + libro.getAutor() + " (" + codigoTexto + ")");

            item.setOnAction(e -> {
                this.libroSeleccionado = libro;
                txtCodigoLibro.setText(codigoTexto);
                txtTituloLibro.setText(libro.getTitulo());
                txtAutorLibro.setText(libro.getAutor());
            });
            sugerenciasPopup.getItems().add(item);
        }

        if (!sugerenciasPopup.isShowing()) {
            sugerenciasPopup.show(campoAsociado, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    // =================================================================================
    // 2. LÓGICA DEL BUSCADOR EN VIVO DE PROFESOR
    // =================================================================================
    @FXML
    public void onBuscarProfesorClick() {
        Dialog<Profesor> dialog = new Dialog<>();
        dialog.setTitle("Buscar Profesor");
        dialog.setHeaderText("Escriba para filtrar y haga doble clic para seleccionar");

        ButtonType btnSeleccionar = new ButtonType("Seleccionar", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSeleccionar, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        TextField txtFiltro = new TextField();
        txtFiltro.setPromptText("Buscar por nombre, apellido, DNI, o contacto...");

        ListView<Profesor> listProfesores = new ListView<>();
        List<Profesor> todosLosProfesores = profesorService.obtenerTodosActivos();
        ObservableList<Profesor> items = FXCollections.observableArrayList(todosLosProfesores);
        listProfesores.setItems(items);

        listProfesores.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Profesor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    String dniStr = (p.getDni() != null) ? " | DNI: " + p.getDni() : "";
                    setText(p.getApellido() + ", " + p.getNombre() + dniStr);
                }
            }
        });

        txtFiltro.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.trim().isEmpty()) {
                items.setAll(todosLosProfesores);
            } else {
                String f = newV.toLowerCase();
                items.setAll(todosLosProfesores.stream()
                        .filter(p -> p.getNombre().toLowerCase().contains(f) ||
                                p.getApellido().toLowerCase().contains(f) ||
                                (p.getDni() != null && p.getDni().contains(f)) ||
                                (p.getEmailTelefono() != null && p.getEmailTelefono().toLowerCase().contains(f)))
                        .toList());
            }
        });

        listProfesores.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listProfesores.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(listProfesores.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        vbox.getChildren().addAll(txtFiltro, listProfesores);
        dialog.getDialogPane().setContent(vbox);

        Node btnSelNode = dialog.getDialogPane().lookupButton(btnSeleccionar);
        btnSelNode.setDisable(true);
        listProfesores.getSelectionModel().selectedItemProperty().addListener((obs, oldS, newS) -> {
            btnSelNode.setDisable(newS == null);
        });

        dialog.setResultConverter(btn -> {
            if (btn == btnSeleccionar) {
                return listProfesores.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(profesor -> {
            this.profesorSeleccionado = profesor;
            txtProfesorSeleccionado.setText(profesor.getApellido() + ", " + profesor.getNombre());
        });
    }

    // =================================================================================
    // 3. LÓGICA DE AGREGAR NUEVO PROFESOR
    // =================================================================================
    @FXML
    public void onAgregarProfesorClick() {
        Dialog<Profesor> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Profesor");
        dialog.setHeaderText("Ingrese los datos del nuevo profesor");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtDni = new TextField();
        txtDni.setPromptText("DNI (Opcional)");
        TextField txtNombre = new TextField();
        TextField txtApellido = new TextField();
        TextField txtContacto = new TextField();

        grid.add(new Label("DNI:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new Label("Nombre (*):"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Apellido (*):"), 0, 2);
        grid.add(txtApellido, 1, 2);
        grid.add(new Label("Contacto:"), 0, 3);
        grid.add(txtContacto, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node guardarButton = dialog.getDialogPane().lookupButton(btnGuardar);
        guardarButton.setDisable(true);

        ChangeListener<String> validador = (observable, oldValue, newValue) -> {
            boolean invalido = txtNombre.getText().trim().isEmpty() ||
                    txtApellido.getText().trim().isEmpty();
            guardarButton.setDisable(invalido);
        };
        txtNombre.textProperty().addListener(validador);
        txtApellido.textProperty().addListener(validador);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                return Profesor.builder()
                        .dni(txtDni.getText().trim())
                        .nombre(txtNombre.getText().trim())
                        .apellido(txtApellido.getText().trim())
                        .emailTelefono(txtContacto.getText().trim())
                        .activo(true)
                        .build();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nuevoProfesor -> {
            try {
                Profesor guardado = profesorService.guardar(nuevoProfesor);
                this.profesorSeleccionado = guardado;
                txtProfesorSeleccionado.setText(guardado.getApellido() + ", " + guardado.getNombre());
                mostrarMensajeExito("Profesor agregado y seleccionado.");
            } catch (Exception e) {
                mostrarMensajeError("Error al guardar profesor: " + e.getMessage());
            }
        });
    }

    // =================================================================================
    // 4. REGISTRO DEL PRÉSTAMO
    // =================================================================================
    @FXML
    public void onRegistrarPrestamoClick() {
        try {
            if (profesorSeleccionado == null) {
                mostrarMensajeError("Debe seleccionar un Profesor.");
                return;
            }

            Libro libroAEnviar = (libroSeleccionado != null) ? libroSeleccionado : Libro.builder()
                    .codigo(txtCodigoLibro.getText().trim())
                    .titulo(txtTituloLibro.getText().trim())
                    .autor(txtAutorLibro.getText().trim())
                    .build();

            if (libroAEnviar.getTitulo() == null || libroAEnviar.getTitulo().isBlank()) {
                mostrarMensajeError("Debe seleccionar o ingresar el Título del libro.");
                return;
            }

            // Obtenemos el ID del usuario real logueado a través del SesionManager
            String usuarioLogueadoId = sesionManager.getUsuarioLogueado().getId();

            prestamoService.registrarPrestamo(
                    profesorSeleccionado.getId(),
                    usuarioLogueadoId,
                    libroAEnviar,
                    dpFechaVencimiento.getValue(),
                    txtMateria.getText().trim(),
                    spnCantidad.getValue()
            );

            mostrarMensajeExito("¡Préstamo registrado correctamente!");
            limpiarFormulario();

        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensajeError(e.getMessage());
        } catch (Exception e) {
            mostrarMensajeError("Error inesperado: " + e.getMessage());
        }
    }

    private void mostrarMensajeExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #155724; -fx-background-color: #d4edda; -fx-padding: 10px; -fx-background-radius: 5px;");
    }

    private void mostrarMensajeError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #721c24; -fx-background-color: #f8d7da; -fx-padding: 10px; -fx-background-radius: 5px;");
    }

    private void limpiarFormulario() {
        profesorSeleccionado = null;
        libroSeleccionado = null;
        txtProfesorSeleccionado.clear();
        txtCodigoLibro.clear();
        txtTituloLibro.clear();
        txtAutorLibro.clear();
        txtMateria.clear();
        spnCantidad.getValueFactory().setValue(1);
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
    }
}