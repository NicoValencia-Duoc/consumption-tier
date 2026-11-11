package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PaginaTabla<T extends ItemConsumption> extends VBox {

    private static final int COMUNES = 5;
    private final TableView<T> tabla = new TableView<>();
    private final FilteredList<T> filtrada;

    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final ComboBox<String> filtroGenero = new ComboBox<>();
    private final CheckBox soloFavoritos = new CheckBox("★ SOLO FAVORITOS");

    private Runnable alAgregar = () -> {};
    private Consumer<T> alEditar = t -> {};
    private Consumer<T> alEliminar = t -> {};
    private Consumer<T> alVerDetalle = t -> {};
    private Consumer<T> alFavorito = t -> {};

    public PaginaTabla(String nombreTipo, List<T> items) {
        setSpacing(14);

        filtrada = new FilteredList<>(FXCollections.observableArrayList(items));
        SortedList<T> ordenada = new SortedList<>(filtrada);
        ordenada.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenada);
        tabla.setPlaceholder(new Label("SIN REGISTROS PARA MOSTRAR"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label titulo = new Label();
        titulo.getStyleClass().add("subtitulo");
        titulo.textProperty().bind(Bindings.size(filtrada).asString(nombreTipo + " · %d REGISTROS"));

        Button favorito = new Button("★ FAVORITO");
        favorito.getStyleClass().add("pestana");
        Button agregar = new Button("+ AGREGAR");
        agregar.getStyleClass().addAll("pestana", "boton-accion");
        Button editar = new Button("EDITAR");
        editar.getStyleClass().add("pestana");
        Button eliminar = new Button("ELIMINAR");
        eliminar.getStyleClass().addAll("pestana", "boton-peligro");
        Button detalle = new Button("DETALLE");
        detalle.getStyleClass().add("pestana");

        for (Button b : List.of(favorito, editar, eliminar, detalle)) {
            b.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        }
        agregar.setOnAction(e -> alAgregar.run());
        editar.setOnAction(e -> alEditar.accept(seleccionado()));
        eliminar.setOnAction(e -> alEliminar.accept(seleccionado()));
        detalle.setOnAction(e -> alVerDetalle.accept(seleccionado()));
        favorito.setOnAction(e -> {
            alFavorito.accept(seleccionado());
            tabla.refresh();
            aplicarFiltros();
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, titulo, espacio, favorito, agregar, editar, eliminar, detalle);
        acciones.setAlignment(Pos.CENTER_LEFT);

        buscador.setPromptText("BUSCAR POR TITULO...");
        buscador.getStyleClass().add("buscador");
        HBox.setHgrow(buscador, Priority.ALWAYS);

        filtroEstado.getItems().add("TODOS");
        for (Estado e : Estado.values()) filtroEstado.getItems().add(e.getEtiqueta());
        filtroEstado.setValue("TODOS");

        filtroGenero.getItems().add("TODOS");
        items.stream().map(ItemConsumption::getGenero).distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER).forEach(filtroGenero.getItems()::add);
        filtroGenero.setValue("TODOS");

        buscador.textProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroEstado.setOnAction(e -> aplicarFiltros());
        filtroGenero.setOnAction(e -> aplicarFiltros());
        soloFavoritos.setOnAction(e -> aplicarFiltros());

        HBox filtros = new HBox(10, buscador, new Label("ESTADO"), filtroEstado,
                new Label("GENERO"), filtroGenero, soloFavoritos);
        filtros.setAlignment(Pos.CENTER_LEFT);

        tabla.getColumns().add(columna("★", i -> i.isFavorito() ? "★" : ""));
        tabla.getColumns().add(columna("TITULO", ItemConsumption::getTitulo));

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
        tabla.getColumns().add(columna("INICIO", i -> fecha(i.getFechaInicio(), "-")));
        tabla.getColumns().add(columna("TÉRMINO", i -> fecha(i.getFechaTermino(), "EN CURSO")));

        tabla.getSortOrder().add(nota);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && seleccionado() != null) alVerDetalle.accept(seleccionado());
        });

        getChildren().addAll(acciones, filtros, tabla);
    }

    private void aplicarFiltros() {
        String texto = buscador.getText().trim().toLowerCase();
        String estado = filtroEstado.getValue();
        String genero = filtroGenero.getValue();

        filtrada.setPredicate(i ->
                (texto.isEmpty() || i.getTitulo().toLowerCase().contains(texto))
                        && (estado == null || estado.equals("TODOS") || i.getEstado().getEtiqueta().equals(estado))
                        && (genero == null || genero.equals("TODOS") || i.getGenero().equals(genero))
                        && (!soloFavoritos.isSelected() || i.isFavorito()));
    }

    private T seleccionado() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    public void setAlAgregar(Runnable accion) { this.alAgregar = accion; }
    public void setAlEditar(Consumer<T> accion) { this.alEditar = accion; }
    public void setAlEliminar(Consumer<T> accion) { this.alEliminar = accion; }
    public void setAlVerDetalle(Consumer<T> accion) { this.alVerDetalle = accion; }
    public void setAlFavorito(Consumer<T> accion) { this.alFavorito = accion; }

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