package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.repository.PrestamoRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    // Para el dashboard de lectura rápida, inyectar el repositorio directamente es aceptable,
    // aunque en sistemas más grandes delegaríamos esto a un ReporteService.
    private final PrestamoRepository prestamoRepository;

    @FXML private Label lblActivos;
    @FXML private Label lblVencidos;

    @FXML private TableView<Prestamo> tablaVencidos;
    @FXML private TableColumn<Prestamo, String> colProfesor;
    @FXML private TableColumn<Prestamo, String> colLibro;
    @FXML private TableColumn<Prestamo, String> colVencimiento;
    @FXML private TableColumn<Prestamo, String> colDiasRetraso;

    private ObservableList<Prestamo> prestamosVencidosList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        cargarDatosDashboard();
    }

    private void configurarTabla() {
        // Extraemos nombre completo del profesor
        colProfesor.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProfesor().getNombre() + " " + cell.getValue().getProfesor().getApellido())
        );

        // Extraemos el título del libro
        colLibro.setCellValueFactory(cell -> {
            Prestamo p = cell.getValue();
            if (!p.getDetalles().isEmpty()) {
                return new SimpleStringProperty(p.getDetalles().get(0).getLibro().getTitulo());
            }
            return new SimpleStringProperty("N/A");
        });

        // Formateamos la fecha de vencimiento
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colVencimiento.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFechaVencimiento().format(formatter))
        );

        // Calculamos los días de retraso
        colDiasRetraso.setCellValueFactory(cell -> {
            LocalDate vencimiento = cell.getValue().getFechaVencimiento();
            long dias = ChronoUnit.DAYS.between(vencimiento, LocalDate.now());
            return new SimpleStringProperty(dias + " días");
        });

        tablaVencidos.setItems(prestamosVencidosList);
    }

    public void cargarDatosDashboard() {
        // Todos los activos
        List<Prestamo> activos = prestamoRepository.findByEstado("ACTIVO");
        lblActivos.setText(String.valueOf(activos.size()));

        // Solo los vencidos (estado ACTIVO y fechaVencimiento menor a hoy)
        List<Prestamo> vencidos = prestamoRepository.findByEstadoAndFechaVencimientoBefore("ACTIVO", LocalDate.now());
        lblVencidos.setText(String.valueOf(vencidos.size()));

        prestamosVencidosList.clear();
        prestamosVencidosList.addAll(vencidos);
    }

    @FXML
    public void onActualizarClick() {
        cargarDatosDashboard();
    }
}