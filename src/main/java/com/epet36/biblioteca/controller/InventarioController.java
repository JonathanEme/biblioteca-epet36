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
    @FXML private TextField txtEditorial;
    @FXML private TextField txtAnioPublicacion;
    @FXML private TextField txtCategoria;
    @FXML private Spinner<Integer> spnCantidadTotal;
    @FXML private Label lblDisponibles;
    @FXML private Label lblMensaje;

    private String libroSeleccionadoId = null;
    private final ObservableList<Libro> librosList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarDatos("");

        txtBuscar.textProperty().addListener((obs, oldV, newV) ->
                cargarDatos(newV != null ? newV.trim() : "")
        );

        tablaLibros.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> seleccionarLibro(newV)
        );

        spnCantidadTotal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> {
            String cod = cell.getValue().getCodigo();
            String isbn = cell.getValue().getIsbn();
            if (cod != null && !cod.isBlank()) return new SimpleStringProperty(cod);
            if (isbn != null && !isbn.isBlank()) return new SimpleStringProperty(isbn);
            return new SimpleStringProperty("-");
        });

        colTitulo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTitulo() != null ? cell.getValue().getTitulo() : ""));

        colAutor.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getAutor() != null ? cell.getValue().getAutor() : ""));

        colStock.setCellValueFactory(cell -> {
            int disp = cell.getValue().getDisponibles() != null ? cell.getValue().getDisponibles() : 0;
            int total = cell.getValue().getCantidadTotal() != null ? cell.getValue().getCantidadTotal() : 0;
            return new SimpleStringProperty(disp + " / " + total);
        });

        // Resalta en amarillo suave si faltan datos en el libro
        tablaLibros.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Libro libro, boolean empty) {
                super.updateItem(libro, empty);
                if (empty || libro == null) {
                    setStyle("");
                } else if (esLibroIncompleto(libro)) {
                    setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                } else {
                    setStyle("");
                }
            }
        });

        tablaLibros.setItems(librosList);
    }

    private boolean esLibroIncompleto(Libro libro) {
        boolean sinCodigoEIsbn = (libro.getCodigo() == null || libro.getCodigo().isBlank()) &&
                (libro.getIsbn() == null || libro.getIsbn().isBlank());
        boolean sinAutor = (libro.getAutor() == null || libro.getAutor().isBlank());
        boolean sinEditorial = (libro.getEditorial() == null || libro.getEditorial().isBlank());
        boolean sinCategoria = (libro.getCategoria() == null || libro.getCategoria().isBlank());
        boolean sinAnio = (libro.getAnio() == null || libro.getAnio() <= 0); // <-- Corregido a getAnio()

        return sinCodigoEIsbn || sinAutor || sinEditorial || sinCategoria || sinAnio;
    }

    private void cargarDatos(String filtro) {
        List<Libro> todos = libroService.obtenerInventario(filtro);
        librosList.clear();
        librosList.addAll(todos);
    }

    private void seleccionarLibro(Libro libro) {
        if (libro != null) {
            libroSeleccionadoId = libro.getId();
            txtCodigo.setText(libro.getCodigo() != null ? libro.getCodigo() : "");
            txtIsbn.setText(libro.getIsbn() != null ? libro.getIsbn() : "");
            txtTitulo.setText(libro.getTitulo() != null ? libro.getTitulo() : "");
            txtAutor.setText(libro.getAutor() != null ? libro.getAutor() : "");
            txtEditorial.setText(libro.getEditorial() != null ? libro.getEditorial() : "");

            Integer anio = libro.getAnio(); // <-- Corregido a getAnio()
            txtAnioPublicacion.setText(anio != null && anio > 0 ? String.valueOf(anio) : "");

            txtCategoria.setText(libro.getCategoria() != null ? libro.getCategoria() : "");

            int total = libro.getCantidadTotal() != null ? libro.getCantidadTotal() : 1;
            spnCantidadTotal.getValueFactory().setValue(total);

            int disp = libro.getDisponibles() != null ? libro.getDisponibles() : 0;
            lblDisponibles.setText("Disponibles actualmente: " + disp);
        }
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (txtTitulo.getText().isBlank()) {
                mostrarMensajeError("El título es obligatorio.");
                return;
            }

            Integer anioParsed = null;
            if (!txtAnioPublicacion.getText().isBlank()) {
                try {
                    anioParsed = Integer.parseInt(txtAnioPublicacion.getText().trim());
                } catch (NumberFormatException e) {
                    mostrarMensajeError("El año de publicación debe ser un número entero válido.");
                    return;
                }
            }

            Libro libro = Libro.builder()
                    .id(libroSeleccionadoId)
                    .codigo(txtCodigo.getText().trim())
                    .isbn(txtIsbn.getText().trim())
                    .titulo(txtTitulo.getText().trim())
                    .autor(txtAutor.getText().trim())
                    .editorial(txtEditorial.getText().trim())
                    .anio(anioParsed) // <-- Corregido a .anio(...)
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
        txtEditorial.clear();
        txtAnioPublicacion.clear();
        txtCategoria.clear();
        spnCantidadTotal.getValueFactory().setValue(1);
        lblDisponibles.setText("-");
        tablaLibros.getSelectionModel().clearSelection();
        lblMensaje.setText("");
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