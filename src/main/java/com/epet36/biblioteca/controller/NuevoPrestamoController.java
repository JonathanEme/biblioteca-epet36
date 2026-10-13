package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.service.PrestamoService;
import com.epet36.biblioteca.service.ProfesorService;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class NuevoPrestamoController {

    private final PrestamoService prestamoService;
    private final ProfesorService profesorService;

    // Campos del Profesor
    @FXML private TextField txtDniProfesor;

    // Campos del Libro
    @FXML private TextField txtCodigoLibro;
    @FXML private TextField txtTituloLibro;
    @FXML private TextField txtAutorLibro;

    // Datos del Préstamo
    @FXML private DatePicker dpFechaVencimiento;

    // Mensajes del sistema
    @FXML private Label lblMensaje;

    @FXML
    public void initialize() {
        // Por defecto, sugerimos la devolución para dentro de 14 días
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
    }

    @FXML
    public void onRegistrarPrestamoClick() {
        try {
            // 1. Validar que los campos obligatorios no estén vacíos
            if (txtDniProfesor.getText().isBlank() || txtTituloLibro.getText().isBlank()) {
                mostrarMensajeError("El DNI del profesor y el Título del libro son obligatorios.");
                return;
            }

            // 2. Buscar al profesor por DNI
            Profesor profesor = profesorService.buscarPorDni(txtDniProfesor.getText().trim());

            // 3. Armar el objeto Libro con los datos ingresados
            Libro libroIngresado = Libro.builder()
                    .codigo(txtCodigoLibro.getText().trim())
                    .titulo(txtTituloLibro.getText().trim())
                    .autor(txtAutorLibro.getText().trim())
                    .build();

            // 4. Registrar el préstamo
            // NOTA: Hasta que no implementemos el Login (UC-01), usamos un ID de usuario fijo (ej. 1L)
            Long usuarioLogueadoId = 1L;

            prestamoService.registrarPrestamo(
                    profesor.getId(),
                    usuarioLogueadoId,
                    libroIngresado,
                    dpFechaVencimiento.getValue()
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
        txtDniProfesor.clear();
        txtCodigoLibro.clear();
        txtTituloLibro.clear();
        txtAutorLibro.clear();
        dpFechaVencimiento.setValue(LocalDate.now().plusDays(14));
    }
}