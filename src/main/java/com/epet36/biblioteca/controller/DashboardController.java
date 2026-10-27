package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.model.PrestamoDetalle;
import com.epet36.biblioteca.model.Profesor;
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

    private final PrestamoRepository prestamoRepository;

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

        tablaVencidos.setItems(prestamosVencidosList);
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