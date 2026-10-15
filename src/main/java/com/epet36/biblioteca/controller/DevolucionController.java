package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.model.PrestamoDetalle;
import com.epet36.biblioteca.service.PrestamoService;
import com.epet36.biblioteca.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DevolucionController {

    private final PrestamoService prestamoService;
    private final UsuarioService usuarioService;

    @FXML private TextField txtBuscarDni;
    @FXML private Label lblMensaje;
    @FXML private Label lblProfesorNombre;

    @FXML private TableView<Prestamo> tablaPrestamos;
    @FXML private TableColumn<Prestamo, String> colLibro;
    @FXML private TableColumn<Prestamo, String> colFechaPrestamo;
    @FXML private TableColumn<Prestamo, String> colFechaVencimiento;
    @FXML private TableColumn<Prestamo, String> colCantidad;
    @FXML private TableColumn<Prestamo, String> colEstado;

    private ObservableList<Prestamo> prestamosList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarTodosLosPrestamos(); // Cargamos todo al abrir

        // MAGIA DE JAVAFX: Escuchamos cada vez que el texto cambia
        txtBuscarDni.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                // Si el usuario borra todo, recargamos la tabla completa
                cargarTodosLosPrestamos();
            } else {
                // Opcional: Si quieres que busque en tiempo real mientras tipea,
                // descomenta la línea de abajo. Si prefieres que presione el botón "Buscar", déjala comentada.
                // onBuscarClick();
            }
        });
    }

    private void cargarTodosLosPrestamos() {
        try {
            List<Prestamo> todos = prestamoService.obtenerTodosLosPrestamos();
            prestamosList.clear();
            prestamosList.addAll(todos);

            lblProfesorNombre.setText("Mostrando el historial completo (" + todos.size() + " registros).");
            lblMensaje.setText(""); // Limpiamos mensajes de error
        } catch (Exception e) {
            mostrarMensajeError("Error al cargar el historial: " + e.getMessage());
        }
    }

    private void configurarTabla() {
        colLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
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

        // Mostrar cantidad: "Devueltos / Totales"
        colCantidad.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (!p.getDetalles().isEmpty()) {
                PrestamoDetalle d = p.getDetalles().get(0);
                int devueltos = d.getCantidadDevuelta() == null ? 0 : d.getCantidadDevuelta();
                return new SimpleStringProperty(devueltos + " / " + d.getCantidad());
            }
            return new SimpleStringProperty("-");
        });

        // Mostrar estado y fecha si ya devolvió
        colEstado.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if ("DEVUELTO".equals(p.getEstado())) {
                return new SimpleStringProperty("Devuelto (" + p.getFechaDevolucion().format(formatter) + ")");
            } else if ("PARCIAL".equals(p.getEstado())) {
                return new SimpleStringProperty("Devolución Parcial");
            }
            return new SimpleStringProperty(p.getEstado()); // Dirá "ACTIVO"
        });

        // Colores en la tabla
        tablaPrestamos.setRowFactory(tv -> new TableRow<Prestamo>() {
            @Override
            protected void updateItem(Prestamo item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("DEVUELTO".equals(item.getEstado())) {
                    setStyle("-fx-background-color: #d4edda;"); // Verde éxito
                } else if ("PARCIAL".equals(item.getEstado())) {
                    setStyle("-fx-background-color: #fff3cd;"); // Amarillo advertencia
                } else {
                    setStyle("");
                }
            }
        });

        tablaPrestamos.setItems(prestamosList);
    }

    @FXML
    public void onBuscarClick() {
        String filtro = txtBuscarDni.getText().trim();
        if (filtro.isEmpty()) {
            cargarTodosLosPrestamos();
            return;
        }

        try {
            List<Prestamo> filtrados = prestamoService.buscarPrestamosMultifiltro(filtro);
            prestamosList.clear();
            prestamosList.addAll(filtrados);

            if (filtrados.isEmpty()) {
                lblProfesorNombre.setText("-");
                mostrarMensajeError("No se encontraron préstamos para: '" + filtro + "'");
            } else {
                lblProfesorNombre.setText("Resultados de la búsqueda: " + filtrados.size());
                mostrarMensajeExito("Filtro aplicado.");
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

        if ("DEVUELTO".equals(seleccionado.getEstado())) {
            mostrarMensajeError("Este préstamo ya se encuentra completamente devuelto.");
            return;
        }

        try {
            PrestamoDetalle detalle = seleccionado.getDetalles().get(0);
            int devueltosActuales = detalle.getCantidadDevuelta() == null ? 0 : detalle.getCantidadDevuelta();
            int cantidadPendiente = detalle.getCantidad() - devueltosActuales;

            // Preguntamos cuántos libros devuelve ahora
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Registrar Devolución");
            dialog.setHeaderText("Devolución de: " + detalle.getLibro().getTitulo());

            ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            grid.add(new Label("Total Prestado:"), 0, 0);
            grid.add(new Label(String.valueOf(detalle.getCantidad())), 1, 0);

            grid.add(new Label("Pendientes de Devolución:"), 0, 1);
            grid.add(new Label(String.valueOf(cantidadPendiente)), 1, 1);

            Spinner<Integer> spnDevuelve = new Spinner<>(1, cantidadPendiente, cantidadPendiente);
            grid.add(new Label("Cantidad que devuelve hoy:"), 0, 2);
            grid.add(spnDevuelve, 1, 2);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnConfirmar) {
                    return spnDevuelve.getValue();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(cantidadADevolver -> {
                try {
                    Long usuarioLogueadoId = usuarioService.obtenerUsuarioPorDefecto().getId();
                    prestamoService.registrarDevolucion(seleccionado.getId(), usuarioLogueadoId, cantidadADevolver);

                    mostrarMensajeExito("¡Devolución registrada con éxito!");
                    onBuscarClick(); // Refresca la tabla para actualizar colores y cantidades
                } catch (Exception e) {
                    mostrarMensajeError("Error al procesar: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            mostrarMensajeError("Error: " + e.getMessage());
        }
    }

    private void mostrarMensajeExito(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #155724; -fx-font-weight: bold;");
    }

    private void mostrarMensajeError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: #721c24; -fx-font-weight: bold;");
    }
}