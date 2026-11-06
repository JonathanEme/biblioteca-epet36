package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.config.SesionManager;
import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.model.PrestamoDetalle;
import com.epet36.biblioteca.model.Profesor;
import com.epet36.biblioteca.repository.PrestamoRepository;
import com.epet36.biblioteca.service.PrestamoService;
import com.epet36.biblioteca.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class DashboardController implements Initializable {

    private final PrestamoRepository prestamoRepository;
    private final PrestamoService prestamoService;
    private final UsuarioService usuarioService;
    private final SesionManager sesionManager;

    @FXML private Label lblActivos;
    @FXML private Label lblVencidos;

    @FXML private TableView<Prestamo> tablaVencidos;
    @FXML private TableColumn<Prestamo, String> colProfesor;
    @FXML private TableColumn<Prestamo, String> colLibro;
    @FXML private TableColumn<Prestamo, String> colVencimiento;
    @FXML private TableColumn<Prestamo, String> colDiasRetraso;

    private final ObservableList<Prestamo> prestamosVencidosList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        cargarDatosDashboard();
    }

    private void configurarTabla() {
        // Hacemos que las columnas de la tabla se adapten de forma elástica al ancho
        tablaVencidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Extraemos nombre completo del profesor con blindaje nulo
        colProfesor.setCellValueFactory(cell -> {
            Prestamo p = cell.getValue();
            if (p != null && p.getProfesor() != null) {
                Profesor prof = p.getProfesor();
                String nom = prof.getNombre() != null ? prof.getNombre() : "";
                String ape = prof.getApellido() != null ? prof.getApellido() : "";
                return new SimpleStringProperty((nom + " " + ape).trim());
            }
            return new SimpleStringProperty("-");
        });

        // Extraemos el título del libro con blindaje nulo
        colLibro.setCellValueFactory(cell -> {
            Prestamo p = cell.getValue();
            if (p != null && p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                PrestamoDetalle d = p.getDetalles().get(0);
                if (d != null && d.getLibro() != null && d.getLibro().getTitulo() != null) {
                    return new SimpleStringProperty(d.getLibro().getTitulo());
                }
            }
            return new SimpleStringProperty("N/A");
        });

        // Formateamos la fecha de vencimiento
        colVencimiento.setCellValueFactory(cell -> {
            Prestamo p = cell.getValue();
            if (p != null && p.getFechaVencimiento() != null) {
                return new SimpleStringProperty(p.getFechaVencimiento().format(formatter));
            }
            return new SimpleStringProperty("-");
        });

        // Calculamos los días de retraso
        colDiasRetraso.setCellValueFactory(cell -> {
            Prestamo p = cell.getValue();
            if (p != null && p.getFechaVencimiento() != null) {
                long dias = ChronoUnit.DAYS.between(p.getFechaVencimiento(), LocalDate.now());
                return new SimpleStringProperty(dias + (dias == 1 ? " día" : " días"));
            }
            return new SimpleStringProperty("-");
        });

        // EVENTO DE DOBLE CLIC EN LA TABLA DE VENCIDOS
        tablaVencidos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Prestamo seleccionado = tablaVencidos.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    mostrarDialogoGestionVencido(seleccionado);
                }
            }
        });

        tablaVencidos.setItems(prestamosVencidosList);
    }

    private void mostrarDialogoGestionVencido(Prestamo prestamo) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Gestión de Préstamo Vencido");

        Profesor prof = prestamo.getProfesor();
        String nombreProfesor = (prof != null) ? prof.getApellido() + ", " + prof.getNombre() : "Desconocido";
        String tituloLibro = (!prestamo.getDetalles().isEmpty() && prestamo.getDetalles().get(0).getLibro() != null)
                ? prestamo.getDetalles().get(0).getLibro().getTitulo() : "Libro";

        dialog.setHeaderText("Profesor: " + nombreProfesor + "\nLibro: " + tituloLibro);

        // Definimos los botones limpios sin correo
        ButtonType btnDevuelto = new ButtonType("✔️ Marcar Devuelto", ButtonBar.ButtonData.OTHER);
        ButtonType btnPerdida = new ButtonType("⚠️ Marcar Pérdida", ButtonBar.ButtonData.OTHER);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(btnDevuelto, btnPerdida, btnCancelar);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("Seleccione una acción correctiva para este préstamo vencido:"));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnDevuelto) return "DEVUELTO";
            if (dialogButton == btnPerdida) return "PERDIDA";
            return null;
        });

        dialog.showAndWait().ifPresent(resultado -> {
            try {
                String usuarioId = usuarioService.obtenerUsuarioPorDefecto().getId();

                if ("DEVUELTO".equals(resultado)) {
                    // Devuelve la cantidad total pendiente
                    PrestamoDetalle det = prestamo.getDetalles().get(0);
                    int pendiente = det.getCantidad() - (det.getCantidadDevuelta() != null ? det.getCantidadDevuelta() : 0);

                    prestamoService.registrarDevolucion(prestamo.getId(), usuarioId, pendiente);
                    mostrarAlerta("Éxito", "Préstamo marcado como devuelto correctamente.");
                    cargarDatosDashboard();

                } else if ("PERDIDA".equals(resultado)) {
                    // Lógica para marcar como pérdida (puedes implementarla en tu servicio de préstamos)
                    mostrarAlerta("Información", "Funcionalidad de marcar como pérdida registrada. (Implementar método en servicio si requiere baja de stock)");
                }
            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo procesar la acción: " + e.getMessage());
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public void cargarDatosDashboard() {
        try {
            List<Prestamo> activos = prestamoRepository.findByEstadoActivoOParcialConDetalles();
            lblActivos.setText(String.valueOf(activos.size()));

            List<Prestamo> vencidos = prestamoRepository.findByEstadoAndFechaVencimientoBeforeConDetalles(LocalDate.now());
            lblVencidos.setText(String.valueOf(vencidos.size()));

            prestamosVencidosList.clear();
            prestamosVencidosList.addAll(vencidos);
        } catch (Exception e) {
            lblActivos.setText("0");
            lblVencidos.setText("0");
            prestamosVencidosList.clear();
        }
    }

    @FXML
    public void onActualizarClick() {
        cargarDatosDashboard();
    }
}