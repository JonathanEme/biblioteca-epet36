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

    private final ObservableList<Prestamo> prestamosList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarTodosLosPrestamos();

        txtBuscarDni.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                cargarTodosLosPrestamos();
            }
        });
    }

    private void cargarTodosLosPrestamos() {
        try {
            List<Prestamo> todos = prestamoService.obtenerTodosLosPrestamos();
            prestamosList.clear();
            prestamosList.addAll(todos);

            lblProfesorNombre.setText("Mostrando el historial completo (" + todos.size() + " registros).");
            lblMensaje.setText("");
        } catch (Exception e) {
            mostrarMensajeError("Error al cargar el historial: " + e.getMessage());
        }
    }

    private void configurarTabla() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p != null && p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                PrestamoDetalle d = p.getDetalles().get(0);
                if (d != null && d.getLibro() != null && d.getLibro().getTitulo() != null) {
                    return new SimpleStringProperty(d.getLibro().getTitulo());
                }
            }
            return new SimpleStringProperty("Sin libro");
        });

        colFechaPrestamo.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p != null && p.getFechaPrestamo() != null) {
                return new SimpleStringProperty(p.getFechaPrestamo().format(formatter));
            }
            return new SimpleStringProperty("-");
        });

        colFechaVencimiento.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p != null && p.getFechaVencimiento() != null) {
                return new SimpleStringProperty(p.getFechaVencimiento().format(formatter));
            }
            return new SimpleStringProperty("-");
        });

        colCantidad.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p != null && p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                PrestamoDetalle d = p.getDetalles().get(0);
                int devueltos = d.getCantidadDevuelta() == null ? 0 : d.getCantidadDevuelta();
                int total = d.getCantidad() == null ? 0 : d.getCantidad();
                return new SimpleStringProperty(devueltos + " / " + total);
            }
            return new SimpleStringProperty("-");
        });

        colEstado.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p == null) return new SimpleStringProperty("-");

            if ("DEVUELTO".equals(p.getEstado())) {
                String fDev = (p.getFechaDevolucion() != null) ? p.getFechaDevolucion().format(formatter) : "s/f";
                return new SimpleStringProperty("Devuelto (" + fDev + ")");
            } else if ("PARCIAL".equals(p.getEstado())) {
                return new SimpleStringProperty("Devolución Parcial");
            }
            return new SimpleStringProperty(p.getEstado() != null ? p.getEstado() : "ACTIVO");
        });

        tablaPrestamos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Prestamo item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("DEVUELTO".equals(item.getEstado())) {
                    setStyle("-fx-background-color: #d4edda;");
                } else if ("PARCIAL".equals(item.getEstado())) {
                    setStyle("-fx-background-color: #fff3cd;");
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
                    // CAMBIO VITAL: usuarioLogueadoId ahora es String (UUID)
                    String usuarioLogueadoId = usuarioService.obtenerUsuarioPorDefecto().getId();
                    prestamoService.registrarDevolucion(seleccionado.getId(), usuarioLogueadoId, cantidadADevolver);

                    mostrarMensajeExito("¡Devolución registrada con éxito!");
                    onBuscarClick();
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