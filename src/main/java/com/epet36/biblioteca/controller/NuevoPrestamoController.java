package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.service.PrestamoService;
import com.epet36.biblioteca.service.ProfesorService;
import com.epet36.biblioteca.service.UsuarioService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.beans.value.ChangeListener;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NuevoPrestamoController {

    private final PrestamoService prestamoService;
    private final ProfesorService profesorService;
    private final UsuarioService usuarioService;

    // Campos UI
    @FXML private ComboBox<Profesor> cmbProfesor;
    @FXML private TextField txtCodigoLibro;
    @FXML private TextField txtTituloLibro;
    @FXML private TextField txtAutorLibro;

    @FXML private TextField txtMateria;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private DatePicker dpFechaVencimiento;
    @FXML private Label lblMensaje;

    @FXML
    public void initialize() {
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
        spnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));

        cargarProfesores();
    }

    private void cargarProfesores() {
        List<Profesor> profesores = profesorService.obtenerTodosActivos();
        cmbProfesor.setItems(FXCollections.observableArrayList(profesores));

        // Configuramos cómo se muestra el profesor en el desplegable
        cmbProfesor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Profesor p) {
                return p == null ? "" : p.getApellido() + ", " + p.getNombre() + " (DNI: " + p.getDni() + ")";
            }
            @Override
            public Profesor fromString(String string) { return null; }
        });
    }

    @FXML
    public void onAgregarProfesorClick() {
        // 1. Crear el cuadro de diálogo
        Dialog<Profesor> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Profesor");
        dialog.setHeaderText("Ingrese los datos del nuevo profesor");

        // 2. Configurar los botones del diálogo (Guardar y Cancelar)
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        // 3. Crear el formulario en una grilla (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtDni = new TextField();
        txtDni.setPromptText("DNI");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre");
        TextField txtApellido = new TextField();
        txtApellido.setPromptText("Apellido");
        TextField txtContacto = new TextField();
        txtContacto.setPromptText("Email o Tel. (Opcional)");

        grid.add(new Label("DNI:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new Label("Nombre (*):"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Apellido (*):"), 0, 2);
        grid.add(txtApellido, 1, 2);
        grid.add(new Label("Contacto:"), 0, 3);
        grid.add(txtContacto, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // 4. Validar que los campos obligatorios no estén vacíos para habilitar "Guardar"
        Node guardarButton = dialog.getDialogPane().lookupButton(btnGuardar);
        guardarButton.setDisable(true); // Deshabilitado por defecto

        ChangeListener<String> validador = (observable, oldValue, newValue) -> {
            boolean invalido = txtNombre.getText().trim().isEmpty() ||
                    txtApellido.getText().trim().isEmpty();
            guardarButton.setDisable(invalido);
        };

        txtNombre.textProperty().addListener(validador);
        txtApellido.textProperty().addListener(validador);

        // 5. Convertir el resultado del formulario a un objeto Profesor
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

        // 6. Mostrar el diálogo y guardar en BD si el usuario confirma
        dialog.showAndWait().ifPresent(nuevoProfesor -> {
            try {
                // Usamos el servicio que ya valida si el DNI está duplicado
                Profesor guardado = profesorService.guardar(nuevoProfesor);

                // Recargamos el ComboBox para que aparezca
                cargarProfesores();

                // Lo seleccionamos automáticamente
                cmbProfesor.setValue(guardado);

                mostrarMensajeExito("Profesor agregado y seleccionado exitosamente.");
            } catch (Exception e) {
                mostrarMensajeError("Error al guardar profesor: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onRegistrarPrestamoClick() {
        try {
            Profesor profesor = cmbProfesor.getValue();
            if (profesor == null || txtTituloLibro.getText().isBlank()) {
                mostrarMensajeError("Debe seleccionar un Profesor e ingresar el Título del libro.");
                return;
            }

            Libro libroIngresado = Libro.builder()
                    .codigo(txtCodigoLibro.getText().trim())
                    .titulo(txtTituloLibro.getText().trim())
                    .autor(txtAutorLibro.getText().trim())
                    .build();

            // 2. REEMPLAZA el usuario en duro por la búsqueda dinámica
            Long usuarioLogueadoId = usuarioService.obtenerUsuarioPorDefecto().getId();

            prestamoService.registrarPrestamo(
                    profesor.getId(),
                    usuarioLogueadoId,
                    libroIngresado,
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
        cmbProfesor.getSelectionModel().clearSelection();
        txtCodigoLibro.clear();
        txtTituloLibro.clear();
        txtAutorLibro.clear();
        txtMateria.clear();
        spnCantidad.getValueFactory().setValue(1);
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
    }
}