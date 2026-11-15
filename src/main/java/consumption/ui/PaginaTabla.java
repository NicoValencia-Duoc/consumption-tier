package consumption.ui;

import consumption.data.EstadoTabla;
import consumption.data.ExportadorCsv;
import consumption.data.Preferencias;
import consumption.model.Estado;
import consumption.model.ItemConsumption;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class PaginaTabla<T extends ItemConsumption> extends VBox {

    private static final PseudoClass MARCADA = PseudoClass.getPseudoClass("marcada");
    private static final PseudoClass DESTELLO = PseudoClass.getPseudoClass("destello");

    private static final String ID_CASILLAS = "casillas";
    private static final String ID_FAVORITO = "fav";
    private static final String ID_TITULO = "titulo";
    private static final String ID_NOTA = "nota";
    private static final int COMUNES = 5;

    private final String nombreTipo;
    private final Preferencias preferencias;
    private final EstadoTabla recordado;

    private final TableView<T> tabla = new TableView<>();
    private final FilteredList<T> filtrada;
    private final ObservableSet<T> marcados = FXCollections.observableSet();
    private final Set<String> ocultasPorDefecto = new HashSet<>();
    private final TableColumn<T, Boolean> columnaCasillas = new TableColumn<>("");
    private boolean restaurando = false;

    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final ComboBox<String> filtroGenero = new ComboBox<>();
    private final CheckBox soloFavoritos = new CheckBox("★ SOLO FAVORITOS");

    private final MenuButton mas = new MenuButton("⋯");
    private final HBox acciones;
    private final HBox barraSeleccion;
    private final Label contador = new Label();
    private final Button botonTodo = new Button();
    private final Button editar = new Button("EDITAR");
    private final Button favorito = new Button("★ FAVORITO");
    private final Button detalle = new Button("DETALLE");
    private final Button eliminar = new Button();

    private Runnable alAgregar = () -> {
    };
    private Consumer<T> alEditar = t -> {
    };
    private Consumer<T> alEliminar = t -> {
    };
    private Consumer<T> alVerDetalle = t -> {
    };
    private Consumer<T> alFavorito = t -> {
    };

    public PaginaTabla(String nombreTipo, List<T> items, Preferencias preferencias) {
        this.nombreTipo = nombreTipo;
        this.preferencias = preferencias;
        this.recordado = preferencias.estadoTabla(nombreTipo);
        setSpacing(14);

        if ("PENDIENTES".equals(nombreTipo)) {
            ocultasPorDefecto.addAll(List.of("nota", "estado", "avance", "inicio", "termino"));
        } else {
            ocultasPorDefecto.addAll(List.of("inicio", "termino"));
        }

        filtrada = new FilteredList<>(FXCollections.observableArrayList(items));
        SortedList<T> ordenada = new SortedList<>(filtrada);
        ordenada.comparatorProperty().bind(
                Bindings.createObjectBinding(this::comparadorConDesempate, tabla.comparatorProperty()));
        tabla.setItems(ordenada);
        tabla.setSortPolicy(t -> t.getItems() instanceof SortedList
                ? Boolean.TRUE
                : TableView.DEFAULT_SORT_POLICY.call(t));
        tabla.setPlaceholder(new Label("SIN REGISTROS · THE MAW TIENE HAMBRE"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label titulo = new Label();
        titulo.getStyleClass().add("subtitulo");
        titulo.textProperty().bind(Bindings.createStringBinding(
                () -> ReglasTabla.textoRegistros(nombreTipo, filtrada.size()), filtrada));

        Button agregar = new Button("+ AGREGAR");
        agregar.getStyleClass().addAll("pestana", "boton-accion");
        agregar.setOnAction(e -> alAgregar.run());
        mas.getStyleClass().addAll("pestana", "menu-mas");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        acciones = new HBox(8, titulo, espacio, agregar, mas);
        acciones.setAlignment(Pos.CENTER_LEFT);
        acciones.setMaxWidth(Double.MAX_VALUE);

        Button cerrar = new Button("✕");
        cerrar.getStyleClass().add("pestana");
        cerrar.setOnAction(e -> marcados.clear());
        contador.getStyleClass().add("contador-seleccion");

        botonTodo.getStyleClass().add("pestana");
        botonTodo.setOnAction(e -> alternarTodo());
        editar.getStyleClass().add("pestana");
        editar.setOnAction(e -> alEditar.accept(unico()));
        favorito.getStyleClass().add("pestana");
        favorito.setOnAction(e -> favoritoEnLote());
        detalle.getStyleClass().add("pestana");
        detalle.setOnAction(e -> alVerDetalle.accept(unico()));
        eliminar.getStyleClass().addAll("pestana", "boton-peligro");
        eliminar.setOnAction(e -> alEliminar.accept(unico()));

        Region espacioSeleccion = new Region();
        HBox.setHgrow(espacioSeleccion, Priority.ALWAYS);
        barraSeleccion = new HBox(8, cerrar, contador, espacioSeleccion,
                botonTodo, editar, favorito, detalle, eliminar);
        barraSeleccion.getStyleClass().add("barra-seleccion");
        barraSeleccion.setAlignment(Pos.CENTER_LEFT);
        barraSeleccion.setMaxWidth(Double.MAX_VALUE);
        barraSeleccion.setVisible(false);

        StackPane zonaAcciones = new StackPane(acciones, barraSeleccion);

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
        filtroEstado.setOnAction(e -> {
            aplicarFiltros();
            guardarFiltros();
        });
        filtroGenero.setOnAction(e -> {
            aplicarFiltros();
            guardarFiltros();
        });
        soloFavoritos.setOnAction(e -> {
            aplicarFiltros();
            guardarFiltros();
        });

        HBox filtros = new HBox(10, buscador, new Label("ESTADO"), filtroEstado,
                new Label("GENERO"), filtroGenero, soloFavoritos);
        filtros.setAlignment(Pos.CENTER_LEFT);

        tabla.getColumns().add(columna(ID_FAVORITO, "★", i -> i.isFavorito() ? "★" : ""));
        tabla.getColumns().add(columna(ID_TITULO, "TITULO", ItemConsumption::getTitulo));

        TableColumn<T, Object> nota = columna(ID_NOTA, "NOTA", ItemConsumption::getNota);
        nota.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object valor, boolean vacio) {
                super.updateItem(valor, vacio);
                int fila = getIndex();
                if (vacio || valor == null || getTableView() == null
                        || fila < 0 || fila >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                T item = getTableView().getItems().get(fila);
                setText(String.format("%.1f", item.getNota()));
                setGraphic(EtiquetaTier.de(item, "tier-chico"));
                setContentDisplay(ContentDisplay.RIGHT);
                setGraphicTextGap(8);
            }
        });

        TableColumn<T, Object> estado = columna("estado", "ESTADO", ItemConsumption::getEstado);
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

        TableColumn<T, Object> avance = columna("avance", "AVANCE (%)", ItemConsumption::getPorcentaje);
        avance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(null);
                T item = (getTableRow() == null) ? null : getTableRow().getItem();
                if (vacio || valor == null || item == null || item.getEstado() == Estado.PENDIENTE) {
                    setGraphic(null);
                    return;
                }
                setGraphic(new BarritaAvance(((Number) valor).doubleValue()));
            }
        });

        tabla.getColumns().add(nota);
        tabla.getColumns().add(estado);
        tabla.getColumns().add(avance);
        tabla.getColumns().add(columna("inicio", "INICIO", i -> fecha(i.getFechaInicio(), "-")));
        tabla.getColumns().add(columna("termino", "TERMINO", i -> fecha(i.getFechaTermino(), "EN CURSO")));

        columnaCasillas.setId(ID_CASILLAS);
        columnaCasillas.setSortable(false);
        columnaCasillas.setResizable(false);
        columnaCasillas.setReorderable(false);
        columnaCasillas.setMinWidth(44);
        columnaCasillas.setPrefWidth(44);
        columnaCasillas.setMaxWidth(44);
        columnaCasillas.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(Boolean.TRUE));
        columnaCasillas.setCellFactory(col -> new CeldaCasilla());
        tabla.getColumns().add(1, columnaCasillas);


        tabla.setRowFactory(tv -> new FilaTabla());
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) Platform.runLater(() -> tabla.getSelectionModel().clearSelection());
        });
        marcados.addListener((SetChangeListener<T>) cambio -> actualizarSeleccion());
        addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE && !marcados.isEmpty()) {
                marcados.clear();
                ev.consume();
            }
        });

        tabla.getSortOrder().addListener((ListChangeListener<TableColumn<T, ?>>) c -> guardarOrden());

        getChildren().addAll(zonaAcciones, filtros, tabla);
        actualizarSeleccion();
    }

    public void montar() {
        restaurando = true;

        List<String> guardadas = recordado.getColumnas();
        for (TableColumn<T, ?> c : tabla.getColumns()) {
            String id = c.getId();
            if (id == null || ID_FAVORITO.equals(id) || ID_TITULO.equals(id) || ID_CASILLAS.equals(id)) continue;
            c.setVisible(guardadas == null ? !ocultasPorDefecto.contains(id) : guardadas.contains(id));
        }

        if (filtroEstado.getItems().contains(recordado.getFiltroEstado())) {
            filtroEstado.setValue(recordado.getFiltroEstado());
        }
        if (filtroGenero.getItems().contains(recordado.getFiltroGenero())) {
            filtroGenero.setValue(recordado.getFiltroGenero());
        }
        soloFavoritos.setSelected(recordado.isSoloFavoritos());
        aplicarFiltros();

        TableColumn<T, ?> orden = columnaPorId(recordado.getOrdenColumna());
        if (orden == null || !orden.isVisible() || !orden.isSortable()) {
            boolean porTitulo = "PENDIENTES".equals(nombreTipo);
            orden = columnaPorId(porTitulo ? ID_TITULO : ID_NOTA);
            orden.setSortType(porTitulo ? TableColumn.SortType.ASCENDING : TableColumn.SortType.DESCENDING);
        } else {
            orden.setSortType(recordado.isOrdenAscendente()
                    ? TableColumn.SortType.ASCENDING : TableColumn.SortType.DESCENDING);
        }
        tabla.getSortOrder().clear();
        tabla.getSortOrder().add(orden);

        construirMenu();
        restaurando = false;
    }

    private void construirMenu() {
        MenuItem exportar = new MenuItem("EXPORTAR CSV");
        exportar.setOnAction(e -> exportarCsv());
        MenuItem rotulo = new MenuItem("COLUMNAS");
        rotulo.setDisable(true);
        mas.getItems().setAll(exportar, new SeparatorMenuItem(), rotulo);

        for (TableColumn<T, ?> c : tabla.getColumns()) {
            String id = c.getId();
            if (id == null || ID_FAVORITO.equals(id) || ID_TITULO.equals(id) || ID_CASILLAS.equals(id)) continue;
            CheckBox casilla = new CheckBox(c.getText());
            casilla.setSelected(c.isVisible());
            casilla.selectedProperty().addListener((obs, antes, ahora) -> alternarColumna(c, ahora));
            mas.getItems().add(new CustomMenuItem(casilla, false));
        }
    }

    private void alternarColumna(TableColumn<T, ?> columna, boolean visible) {
        columna.setVisible(visible);
        if (!visible && tabla.getSortOrder().contains(columna)) {
            tabla.getSortOrder().remove(columna);
            if (tabla.getSortOrder().isEmpty()) {
                TableColumn<T, ?> titulo = columnaPorId(ID_TITULO);
                titulo.setSortType(TableColumn.SortType.ASCENDING);
                tabla.getSortOrder().add(titulo);
            }
        }
        guardarColumnas();
    }

    private void guardarColumnas() {
        if (restaurando) return;
        List<String> ids = new ArrayList<>();
        for (TableColumn<T, ?> c : tabla.getColumns()) {
            String id = c.getId();
            if (id == null || ID_FAVORITO.equals(id) || ID_TITULO.equals(id) || ID_CASILLAS.equals(id)) continue;
            if (c.isVisible()) ids.add(id);
        }
        recordado.setColumnas(ids);
        preferencias.guardar();
    }

    private void guardarFiltros() {
        if (restaurando) return;
        recordado.setFiltroEstado(filtroEstado.getValue());
        recordado.setFiltroGenero(filtroGenero.getValue());
        recordado.setSoloFavoritos(soloFavoritos.isSelected());
        preferencias.guardar();
    }

    private void guardarOrden() {
        if (restaurando) return;
        if (tabla.getSortOrder().isEmpty()) {
            recordado.setOrdenColumna(null);
        } else {
            TableColumn<T, ?> c = tabla.getSortOrder().get(0);
            recordado.setOrdenColumna(c.getId());
            recordado.setOrdenAscendente(c.getSortType() == TableColumn.SortType.ASCENDING);
        }
        preferencias.guardar();
    }

    private TableColumn<T, ?> columnaPorId(String id) {
        if (id == null) return null;
        for (TableColumn<T, ?> c : tabla.getColumns()) {
            if (id.equals(c.getId())) return c;
        }
        return null;
    }

    private void alternar(T item) {
        if (!marcados.remove(item)) marcados.add(item);
    }

    private void alternarTodo() {
        if (marcados.containsAll(filtrada)) marcados.clear();
        else marcados.addAll(filtrada);
    }

    private T unico() {
        return marcados.size() == 1 ? marcados.iterator().next() : null;
    }

    private void favoritoEnLote() {
        List<T> lista = new ArrayList<>(marcados);
        boolean todas = lista.stream().allMatch(ItemConsumption::isFavorito);
        for (T i : lista) {
            if (ReglasTabla.debeAlternarFavorito(i.isFavorito(), todas)) alFavorito.accept(i);
        }
        tabla.refresh();
        aplicarFiltros();
    }

    private void actualizarSeleccion() {
        int n = marcados.size();
        boolean activo = n > 0;
        boolean estabaActiva = barraSeleccion.isVisible();

        acciones.setVisible(!activo);
        barraSeleccion.setVisible(activo);
        if (!activo) return;

        contador.setText(ReglasTabla.textoSeleccionados(n));
        botonTodo.setText(ReglasTabla.textoSeleccionarTodo(marcados.containsAll(filtrada)));
        editar.setDisable(n != 1);
        detalle.setDisable(n != 1);
        eliminar.setText(ReglasTabla.textoEliminar(n));
        eliminar.setDisable(n != 1);
        if (!estabaActiva) tabla.requestFocus();
    }

    private class CeldaCasilla extends TableCell<T, Boolean> {
        private static final boolean SIEMPRE_VISIBLES = true;
        private final CheckBox casilla = new CheckBox();
        private final SetChangeListener<T> escucha = cambio -> pintar();

        CeldaCasilla() {
            setAlignment(Pos.CENTER);
            casilla.setMouseTransparent(true);
            casilla.setFocusTraversable(false);
            marcados.addListener(escucha);
        }

        @Override
        protected void updateItem(Boolean valor, boolean vacio) {
            super.updateItem(valor, vacio);
            pintar();
        }

        private void pintar() {
            T item = (getTableRow() == null) ? null : getTableRow().getItem();
            if (isEmpty() || item == null || (!SIEMPRE_VISIBLES && marcados.isEmpty())) {
                setGraphic(null);
                return;
            }
            casilla.setSelected(marcados.contains(item));
            setGraphic(casilla);
        }
    }

    private class FilaTabla extends TableRow<T> {

        FilaTabla() {
            hoverProperty().addListener((obs, antes, ahora) -> pintar());
            setOnMouseClicked(e -> {
                if (isEmpty() || getItem() == null || e.getButton() != MouseButton.PRIMARY) return;
                T item = getItem();
                alternar(item);
                if (e.getClickCount() == 2) {
                    alternar(item);
                    alVerDetalle.accept(item);
                }
            });
            marcados.addListener((SetChangeListener<T>) cambio -> pintar());
        }

        @Override
        protected void updateItem(T item, boolean vacio) {
            super.updateItem(item, vacio);
            setTooltip((vacio || item == null) ? null : TooltipoComentario.de(item.getComentario()));
            pintar();
        }

        private void pintar() {
            boolean conItem = !isEmpty() && getItem() != null;
            pseudoClassStateChanged(DESTELLO, conItem && isHover());
            pseudoClassStateChanged(MARCADA, conItem && marcados.contains(getItem()));
        }
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

        marcados.retainAll(new HashSet<>(filtrada));
    }

    private void exportarCsv() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar a CSV");
        selector.setInitialFileName(nombreTipo.toLowerCase() + ".csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File archivo = selector.showSaveDialog(getScene().getWindow());
        if (archivo == null) return;

        Alert alerta;
        try {
            ExportadorCsv.exportar(tabla, archivo.toPath());
            alerta = new Alert(Alert.AlertType.INFORMATION,
                    "Se exportaron " + tabla.getItems().size() + " filas a " + archivo.getName());
        } catch (IOException ex) {
            alerta = new Alert(Alert.AlertType.ERROR, "No se pudo guardar el archivo: " + ex.getMessage());
        }
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(getScene().getStylesheets());
        alerta.showAndWait();
    }

    private Comparator<T> comparadorConDesempate() {
        Comparator<T> alfabetico = Comparator.comparing(ItemConsumption::getTitulo, String.CASE_INSENSITIVE_ORDER);
        Comparator<T> base = tabla.getComparator();
        return (base == null) ? alfabetico : base.thenComparing(alfabetico);
    }

    public void setAlAgregar(Runnable accion) {
        this.alAgregar = accion;
    }

    public void setAlEditar(Consumer<T> accion) {
        this.alEditar = accion;
    }

    public void setAlEliminar(Consumer<T> accion) {
        this.alEliminar = accion;
    }

    public void setAlVerDetalle(Consumer<T> accion) {
        this.alVerDetalle = accion;
    }

    public void setAlFavorito(Consumer<T> accion) {
        this.alFavorito = accion;
    }

    public void agregarColumna(String id, String titulo, Function<T, Object> extractor, boolean visiblePorDefecto) {
        tabla.getColumns().add(tabla.getColumns().size() - COMUNES, columna(id, titulo, extractor));
        if (!visiblePorDefecto) ocultasPorDefecto.add(id);
    }

    private TableColumn<T, Object> columna(String id, String titulo, Function<T, Object> extractor) {
        TableColumn<T, Object> c = new TableColumn<>(titulo);
        c.setId(id);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(extractor.apply(d.getValue())));
        c.sortTypeProperty().addListener((obs, antes, ahora) -> guardarOrden());
        return c;
    }

    private static String fecha(LocalDate f, String siVacia) {
        return (f == null) ? siVacia : f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}