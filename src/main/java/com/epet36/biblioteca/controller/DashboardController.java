package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Prestamo;
import com.epet36.biblioteca.repository.PrestamoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

@Component // Importante: Spring gestionará este controlador
public class DashboardController implements Initializable {

    // Inyectamos lo que necesitamos consultar. (Para reportes directos, usar repositorios está bien)
    private final PrestamoRepository prestamoRepository;

    @FXML private Label lblActivos;
    @FXML private Label lblVencidos;

    @FXML private TableView<Prestamo> tablaVencidos;
    @FXML private TableColumn<Prestamo, String> colProfesor;
    @FXML private TableColumn<Prestamo, LocalDate> colVencimiento;
    @FXML private TableColumn<Prestamo, String> colEstado;

    private ObservableList<Prestamo> prestamosVencidosList;

    // Constructor autowired por Spring
    public DashboardController(PrestamoRepository prestamoRepository) {
        this.prestamoRepository = prestamoRepository;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
        cargarDatosDashboard();
    }

    private void configurarTabla() {
        // En un caso real, 'Profesor' es un objeto, así que deberías usar SimpleStringProperty
        // para extraer el nombre, pero esto es el setup básico.
        colProfesor.setCellValueFactory(new PropertyValueFactory<>("profesor"));
        colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        prestamosVencidosList = FXCollections.observableArrayList();
        tablaVencidos.setItems(prestamosVencidosList);
    }

    public void cargarDatosDashboard() {
        // Consultar activos
        List<Prestamo> activos = prestamoRepository.findByEstado("ACTIVO");
        lblActivos.setText(String.valueOf(activos.size()));

        // Consultar vencidos (Activos y con fechaVencimiento menor a hoy)
        List<Prestamo> vencidos = prestamoRepository.findByEstadoAndFechaVencimientoBefore("ACTIVO", LocalDate.now());
        lblVencidos.setText(String.valueOf(vencidos.size()));

        // Actualizar la tabla
        prestamosVencidosList.clear();
        prestamosVencidosList.addAll(vencidos);
    }

    @FXML
    public void onActualizarClick() {
        cargarDatosDashboard();
    }
}