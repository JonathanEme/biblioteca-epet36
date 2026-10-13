package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.service.PrestamoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DevolucionController {

    private final PrestamoService prestamoService;

    @FXML private TextField txtBuscarDni;
    @FXML private Label lblMensaje;
    @FXML private Label lblProfesorNombre;

    @FXML private TableView<Prestamo> tablaPrestamos;
    @FXML private TableColumn<Prestamo, String> colLibro;
    @FXML private TableColumn<Prestamo, String> colFechaPrestamo;
    @FXML private TableColumn<Prestamo, String> colFechaVencimiento;

    private ObservableList<Prestamo> prestamosList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
    }

    private void configurarTabla() {
        // Configuramos cómo se extraen los datos para cada columna
        colLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            // Asumimos que un préstamo tiene al menos 1 detalle
            if (!p.getDetalles().isEmpty()) {
                return new SimpleStringProperty(p.getDetalles().get(0).getLibro().getTitulo());
            }
            return new SimpleStringProperty("Sin libro");
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colFechaPrestamo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFechaPrestamo().format(formatter)));

        colFechaVencimiento.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFechaVencimiento().format(formatter)));

        tablaPrestamos.setItems(prestamosList);
    }

    @FXML
    public void onBuscarClick() {
        String dni = txtBuscarDni.getText().trim();
        if (dni.isEmpty()) {
            mostrarMensajeError("Por favor ingrese un DNI.");
            return;
        }

        try {
            List<Prestamo> activos = prestamoService.buscarPrestamosActivosPorDni(dni);
            prestamosList.clear();
            prestamosList.addAll(activos);

            if (activos.isEmpty()) {
                lblProfesorNombre.setText("-");
                mostrarMensajeError("No se encontraron préstamos activos para el DNI: " + dni);
            } else {
                // Tomamos el nombre del profesor del primer resultado para mostrarlo
                String nombreCompleto = activos.get(0).getProfesor().getNombre() + " " + activos.get(0).getProfesor().getApellido();
                lblProfesorNombre.setText(nombreCompleto);
                mostrarMensajeExito("Préstamos encontrados: " + activos.size());
            }
        } catch (Exception e) {
            mostrarMensajeError("Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    public void onDevolverClick() {
        Prestamo seleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarMensajeError("Debe seleccionar un préstamo de la tabla para devolver.");
            return;
        }

        try {
            // Usuario en duro por ahora hasta que implementemos Login (UC-01)
            Long usuarioLogueadoId = 1L;
            prestamoService.registrarDevolucion(seleccionado.getId(), usuarioLogueadoId);

            mostrarMensajeExito("¡Libro devuelto con éxito! Disponibilidad actualizada.");

            // Recargamos la tabla para que desaparezca el que acabamos de devolver
            onBuscarClick();

        } catch (Exception e) {
            mostrarMensajeError("Error al procesar la devolución: " + e.getMessage());
        }
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