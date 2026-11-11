package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Function;

public class PaginaTabla <T extends ItemConsumption> extends VBox{

    private static final int COMUNES = 5;
    private final TableView<T> tabla = new TableView<>();

    private Runnable alAgregar = () -> {};
    private Consumer<T> alEditar = t -> {};
    private Consumer<T> alEliminar = t -> {};
    private Consumer<T> alVerDetalle = t -> {};

    public PaginaTabla(String nombreTipo, List<T> items) {
        setSpacing(14);
        Label titulo = new Label(nombreTipo + " · " + items.size() + " REGISTROS");

        titulo.getStyleClass().add("subtitulo");

        Button agregar = new Button("+ AGREGAR");
        agregar.getStyleClass().addAll("pestana", "boton-accion");
        Button editar = new Button("EDITAR");
        editar.getStyleClass().add("pestana");
        Button eliminar = new Button("ELIMINAR");
        eliminar.getStyleClass().addAll("pestana", "boton-peligro");

        Button detalle = new Button("DETALLE");
        detalle.getStyleClass().add("pestana");
        detalle.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        detalle.setOnAction(e -> alVerDetalle.accept(tabla.getSelectionModel().getSelectedItem()));

        editar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        eliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        agregar.setOnAction(e -> alAgregar.run());
        editar.setOnAction(e -> alEditar.accept(tabla.getSelectionModel().getSelectedItem()));
        eliminar.setOnAction(e -> alEliminar.accept(tabla.getSelectionModel().getSelectedItem()));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, titulo, espacio, agregar, editar, eliminar, detalle);
        acciones.setAlignment(Pos.CENTER_LEFT);

        tabla.setItems(FXCollections.observableArrayList(items));
        tabla.setPlaceholder(new Label("SIN REGISTROS TODAVÍA · USA + AGREGAR"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().add(columna("TÍTULO", ItemConsumption::getTitulo));

        TableColumn<T, Object> nota = columna("NOTA", ItemConsumption::getNota);
        nota.setSortType(TableColumn.SortType.DESCENDING);

        TableColumn<T, Object> estado = columna("ESTADO", ItemConsumption::getEstado);
        estado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object valor, boolean vacio) {
                super.updateItem(valor, vacio);
                for (Estado e : Estado.values()) getStyleClass().remove(e.getClaseCss());
                if (vacio || valor == null) {
                    setText(null);
                    return;
                }
                Estado e = (Estado) valor;
                setText(e.getEtiqueta());
                getStyleClass().add(e.getClaseCss());
            }
        });

        tabla.getColumns().add(nota);
        tabla.getColumns().add(estado);
        tabla.getColumns().add(columna("AVANCE (%)", ItemConsumption::getPorcentaje));
        tabla.getColumns().add(columna("INICIO", i -> fecha(i.getFechaInicio(), "—")));
        tabla.getColumns().add(columna("TÉRMINO", i -> fecha(i.getFechaTermino(), "EN CURSO")));

        tabla.getSortOrder().add(nota);
        tabla.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && tabla.getSelectionModel().getSelectedItem() != null) {
                alVerDetalle.accept(tabla.getSelectionModel().getSelectedItem());
            }
        });
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getChildren().addAll(acciones, tabla);
    }

    public void setAlAgregar(Runnable accion) { this.alAgregar = accion; }
    public void setAlEditar(Consumer<T> accion) { this.alEditar = accion; }
    public void setAlEliminar(Consumer<T> accion) { this.alEliminar = accion; }
    public void setAlVerDetalle(Consumer<T> accion) { this.alVerDetalle = accion; }

    public void agregarColumna(String titulo, Function<T, Object> extractor) {
        tabla.getColumns().add(tabla.getColumns().size() - COMUNES, columna(titulo, extractor));
    }

    private TableColumn<T, Object> columna(String titulo, Function<T, Object> extractor) {
        TableColumn<T, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(extractor.apply(d.getValue())));
        return c;
    }

    private static String fecha(LocalDate f, String siVacia) {
        return (f == null) ? siVacia : f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}

