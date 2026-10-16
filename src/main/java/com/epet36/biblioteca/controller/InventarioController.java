package com.epet36.biblioteca.controller;

import com.epet36.biblioteca.model.Libro;
import com.epet36.biblioteca.service.LibroService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventarioController {

    private final LibroService libroService;

    @FXML private TextField txtBuscar;
    @FXML private TableView<Libro> tablaLibros;
    @FXML private TableColumn<Libro, String> colCodigo;
    @FXML private TableColumn<Libro, String> colTitulo;
    @FXML private TableColumn<Libro, String> colAutor;
    @FXML private TableColumn<Libro, String> colStock;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtIsbn;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAutor;
    @FXML private TextField txtCategoria;
    @FXML private Spinner<Integer> spnCantidadTotal;
    @FXML private Label lblDisponibles;
    @FXML private Label lblMensaje;

    private Long libroSeleccionadoId = null;
    private ObservableList<Libro> librosList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarDatos("");

        txtBuscar.textProperty().addListener((obs, oldV, newV) -> {
            cargarDatos(newV.trim());
        });

        tablaLibros.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> seleccionarLibro(newV)
        );

        spnCantidadTotal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> {
            String cod = cell.getValue().getCodigo();
            return new SimpleStringProperty(cod != null ? cod : cell.getValue().getIsbn());
        });
        colTitulo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitulo()));
        colAutor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAutor()));
        colStock.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDisponibles() + " / " + cell.getValue().getCantidadTotal())
        );

        tablaLibros.setItems(librosList);
    }

    private void cargarDatos(String filtro) {
        List<Libro> todos = libroService.obtenerInventario(filtro);
        librosList.clear();
        librosList.addAll(todos);
    }

    private void seleccionarLibro(Libro libro) {
        if (libro != null) {
            libroSeleccionadoId = libro.getId();
            txtCodigo.setText(libro.getCodigo());
            txtIsbn.setText(libro.getIsbn());
            txtTitulo.setText(libro.getTitulo());
            txtAutor.setText(libro.getAutor());
            txtCategoria.setText(libro.getCategoria());
            spnCantidadTotal.getValueFactory().setValue(libro.getCantidadTotal());
            lblDisponibles.setText("Disponibles actualmente: " + libro.getDisponibles());
        }
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (txtTitulo.getText().isBlank()) {
                mostrarMensajeError("El título es obligatorio.");
                return;
            }

            Libro libro = Libro.builder()
                    .id(libroSeleccionadoId)
                    .codigo(txtCodigo.getText().trim())
                    .isbn(txtIsbn.getText().trim())
                    .titulo(txtTitulo.getText().trim())
                    .autor(txtAutor.getText().trim())
                    .categoria(txtCategoria.getText().trim())
                    .cantidadTotal(spnCantidadTotal.getValue())
                    .build();

            libroService.actualizarLibroMantenimiento(libro);
            mostrarMensajeExito("Libro guardado correctamente en el inventario.");

            onLimpiarClick();
            cargarDatos(txtBuscar.getText());

        } catch (Exception e) {
            mostrarMensajeError(e.getMessage());
        }
    }

    @FXML
    public void onLimpiarClick() {
        libroSeleccionadoId = null;
        txtCodigo.clear();
        txtIsbn.clear();
        txtTitulo.clear();
        txtAutor.clear();
        txtCategoria.clear();
        spnCantidadTotal.getValueFactory().setValue(1);
        lblDisponibles.setText("-");
        tablaLibros.getSelectionModel().clearSelection();
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