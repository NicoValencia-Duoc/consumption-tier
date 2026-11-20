package consumption.ui;

import consumption.data.EstadoTabla;
import consumption.data.ExportadorCsv;
import consumption.data.GestorImagenes;
import consumption.data.Preferencias;
import consumption.model.Estado;
import consumption.model.Formato;
import consumption.model.ItemConsumption;
import consumption.model.Transicion;

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
import consumption.model.Numeracion;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
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
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PaginaTabla<T extends ItemConsumption> extends VBox {

    private static final PseudoClass MARCADA = PseudoClass.getPseudoClass("marcada");
    private static final PseudoClass DESTELLO = PseudoClass.getPseudoClass("destello");

    private static final String ID_CASILLAS = "casillas";
    private static final String ID_FAVORITO = "fav";
    private static final String ID_NUMERO = "id";
    private static final String ID_PORTADA = "portada";
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

    private final CampoBusqueda buscador = new CampoBusqueda();
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
    private final Button empezar = new Button(Transicion.EMPEZAR.getEtiqueta());
    private final Button terminar = new Button(Transicion.TERMINAR.getEtiqueta());
    private final Button abandonar = new Button(Transicion.ABANDONAR.getEtiqueta());

    private ContextMenu menuAbierto;

    private Runnable alAgregar = () -> {};
    private Consumer<T> alEditar = t -> {};
    private Consumer<List<T>> alEliminar = lote -> {};
    private BiConsumer<T, Transicion> alCambiarEstado = (t, transicion) -> {};
    private Consumer<T> alVerDetalle = t -> {
    };
    private Consumer<T> alFavorito = t -> {
    };
    private Consumer<List<T>> alFavoritosEnLote;

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
        tabla.setPlaceholder(new Label("( ◉ ) ALGO RESPIRA AQUI · SIN REGISTROS"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label cantidad = new Label();
        cantidad.textProperty().bind(Bindings.createStringBinding(
                () -> "// " + ReglasTabla.textoCantidad(filtrada.size()), filtrada));
        HBox titulo = Terminal.titulo(nombreTipo, cantidad);

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
        cerrar.getStyleClass().addAll("pestana", "cerrar-plano");
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
        eliminar.setOnAction(e -> alEliminar.accept(new ArrayList<>(marcados)));

        empezar.getStyleClass().add("pestana");
        empezar.setOnAction(e -> alCambiarEstado.accept(unico(), Transicion.EMPEZAR));
        terminar.getStyleClass().addAll("pestana", "boton-accion");
        terminar.setOnAction(e -> alCambiarEstado.accept(unico(), Transicion.TERMINAR));
        abandonar.getStyleClass().add("pestana");
        abandonar.setOnAction(e -> alCambiarEstado.accept(unico(), Transicion.ABANDONAR));

        Terminal.corchetes(agregar);
        Terminal.corchetes(mas);
        for (Button b : new Button[] {botonTodo, editar, empezar, terminar, abandonar, favorito, detalle, eliminar}) {
            Terminal.corchetes(b);
        }

        barraSeleccion = new HBox(8, cerrar, contador,
                botonTodo, editar, empezar, terminar, abandonar, favorito, detalle, eliminar);
        barraSeleccion.getStyleClass().add("barra-seleccion");

        barraSeleccion.setPadding(new javafx.geometry.Insets(0, 0, 0, 10));
        barraSeleccion.setAlignment(Pos.CENTER_LEFT);
        barraSeleccion.setMinWidth(Region.USE_PREF_SIZE);
        for (Button b : new Button[] {cerrar, botonTodo, editar, empezar, terminar,
                abandonar, favorito, detalle, eliminar}) {
            b.setMinWidth(Region.USE_PREF_SIZE);
        }
        Region separadorBarra = new Region();
        separadorBarra.setMinHeight(16);
        separadorBarra.setPrefHeight(16);
        separadorBarra.setMaxHeight(16);
        VBox contenidoSeleccion = new VBox(0, barraSeleccion, separadorBarra);
        contenidoSeleccion.setMinWidth(Region.USE_PREF_SIZE);
        ScrollPane desplazamientoSeleccion = new ScrollPane(contenidoSeleccion);
        desplazamientoSeleccion.setFitToHeight(false);
        desplazamientoSeleccion.setFitToWidth(false);
        desplazamientoSeleccion.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamientoSeleccion.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        desplazamientoSeleccion.setPannable(true);
        desplazamientoSeleccion.setMinWidth(0);
        desplazamientoSeleccion.setMaxWidth(Double.MAX_VALUE);
        desplazamientoSeleccion.setVisible(false);

        desplazamientoSeleccion.setPrefHeight(62);
        desplazamientoSeleccion.setMinHeight(62);
        desplazamientoSeleccion.setMaxHeight(62);
        StackPane zonaAcciones = new StackPane(acciones, desplazamientoSeleccion);
        zonaAcciones.setAlignment(Pos.TOP_LEFT);
        zonaAcciones.setMinHeight(62);
        zonaAcciones.setPrefHeight(62);
        zonaAcciones.setMaxHeight(62);
        StackPane.setAlignment(acciones, Pos.TOP_LEFT);
        StackPane.setAlignment(desplazamientoSeleccion, Pos.TOP_LEFT);
        barraSeleccion.visibleProperty().addListener((obs, antes, activo) ->
                desplazamientoSeleccion.setVisible(activo));

        buscador.setPromptText("buscar --titulo");
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

        TableColumn<T, Object> colNumero = columna(ID_NUMERO, "ID", i -> Numeracion.etiqueta(i.getNumero()));
        colNumero.setPrefWidth(64);
        colNumero.setMaxWidth(90);
        tabla.getColumns().add(colNumero);
        tabla.getColumns().add(columna(ID_FAVORITO, "★", i -> i.isFavorito() ? "★" : ""));
        TableColumn<T, Object> columnaTitulo = columna(ID_TITULO, "TITULO", ItemConsumption::getTitulo);
        columnaTitulo.setMinWidth(180);
        columnaTitulo.setPrefWidth(230);
        tabla.getColumns().add(columnaTitulo);

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
                setText(Formato.decimal(item.getNota()));
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
                setText((e == Estado.PENDIENTE ? "▒ " : "● ") + e.getEtiqueta());
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
        TableColumn<T, Object> colInicio = columna("inicio", "INICIO", PaginaTabla::celdaInicio);
        TableColumn<T, Object> colTermino = columna("termino", "TERMINO", PaginaTabla::celdaTermino);
        ordenarPorFecha(colInicio);
        ordenarPorFecha(colTermino);
        tabla.getColumns().add(colInicio);
        tabla.getColumns().add(colTermino);

        columnaCasillas.setId(ID_CASILLAS);
        columnaCasillas.setSortable(false);
        columnaCasillas.setResizable(false);
        columnaCasillas.setReorderable(false);
        columnaCasillas.setMinWidth(44);
        columnaCasillas.setPrefWidth(44);
        columnaCasillas.setMaxWidth(44);
        columnaCasillas.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(Boolean.TRUE));
        columnaCasillas.setCellFactory(col -> new CeldaCasilla());
        tabla.getColumns().add(0, columnaCasillas);
        tabla.getColumns().add(1, crearColumnaPortada());
        tabla.setFixedCellSize(preferencias.isTablaCompacta()
                ? Math.max(36, Math.ceil(36 * preferencias.getEscalaTexto() / 100.0))
                : Math.max(48, Math.ceil(48 * preferencias.getEscalaTexto() / 100.0)));


        tabla.setRowFactory(tv -> new FilaTabla());
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        final double anchoTituloBase = 230;
        tabla.widthProperty().addListener((obs, antes, ahora) -> {
            double disponible = ahora.doubleValue() - 20;
            if (disponible <= 0) return;

            double otros = 0;
            for (TableColumn<T, ?> c : tabla.getColumns()) {
                if (c != columnaTitulo && c.isVisible()) {
                    otros += c.getWidth() > 0 ? c.getWidth() : c.getPrefWidth();
                }
            }

            columnaTitulo.setPrefWidth(Math.max(anchoTituloBase, disponible - otros));
        });

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
            if (id == null || esFija(id)) continue;
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
            if (id == null || esFija(id)) continue;
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
            if (id == null || esFija(id)) continue;
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

    private static boolean esFija(String id) {
        return ID_FAVORITO.equals(id) || ID_TITULO.equals(id) || ID_NUMERO.equals(id)
                || ID_CASILLAS.equals(id) || ID_PORTADA.equals(id);
    }

    private void abrirMenu(T item, TableRow<T> fila, double x, double y) {
        if (menuAbierto != null) menuAbierto.hide();
        List<T> objetivo = marcados.contains(item) ? new ArrayList<>(marcados) : List.of(item);
        menuAbierto = new MenuAcciones<>(objetivo, alEditar, this::favoritoEn, alCambiarEstado, alEliminar);
        menuAbierto.show(fila, x, y);
    }

    private TableColumn<T, Object> crearColumnaPortada() {
        TableColumn<T, Object> c = columna(ID_PORTADA, "", i -> i);
        c.setSortable(false);
        c.setResizable(false);
        c.setReorderable(false);
        c.setMinWidth(52);
        c.setPrefWidth(52);
        c.setMaxWidth(52);
        c.setCellFactory(col -> new CeldaMiniatura());
        return c;
    }

    private T unico() {
        return marcados.size() == 1 ? marcados.iterator().next() : null;
    }

    private void favoritoEnLote() {
        favoritoEn(new ArrayList<>(marcados));
    }

    private void favoritoEn(List<T> lista) {
        boolean todas = lista.stream().allMatch(ItemConsumption::isFavorito);
        List<T> cambiar = lista.stream()
                .filter(i -> ReglasTabla.debeAlternarFavorito(i.isFavorito(), todas))
                .toList();
        if (alFavoritosEnLote != null) {
            if (!cambiar.isEmpty()) alFavoritosEnLote.accept(cambiar);
        } else {
            for (T i : cambiar) alFavorito.accept(i);
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
        T unica = unico();
        editar.setDisable(unica == null);
        detalle.setDisable(unica == null);
        empezar.setDisable(unica == null || !Transicion.EMPEZAR.permitidaDesde(unica.getEstado()));
        terminar.setDisable(unica == null || !Transicion.TERMINAR.permitidaDesde(unica.getEstado()));
        abandonar.setDisable(unica == null || !Transicion.ABANDONAR.permitidaDesde(unica.getEstado()));
        eliminar.setText(ReglasTabla.textoEliminar(n));
        eliminar.setDisable(false);
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

    private class CeldaMiniatura extends TableCell<T, Object> {
        private final ImageView vista = new ImageView();
        private final StackPane marco = new StackPane(vista);

        CeldaMiniatura() {
            marco.getStyleClass().add("miniatura-fila");
            int ancho = preferencias.isTablaCompacta() ? 24 : 32;
            int alto = preferencias.isTablaCompacta() ? 30 : 42;
            marco.setMinSize(ancho, alto);
            marco.setPrefSize(ancho, alto);
            marco.setMaxSize(ancho, alto);
            vista.setFitWidth(ancho - 4);
            vista.setFitHeight(alto - 4);
            vista.setPreserveRatio(true);
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Object valor, boolean vacio) {
            super.updateItem(valor, vacio);
            T item = (getTableRow() == null) ? null : getTableRow().getItem();
            if (vacio || item == null) {
                setGraphic(null);
                return;
            }
            vista.setImage(GestorImagenes.imagenDe(item,
                    preferencias.isTablaCompacta() ? 20 : 28,
                    preferencias.isTablaCompacta() ? 26 : 38));
            setGraphic(marco);
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
            setOnContextMenuRequested(e -> {
                if (isEmpty() || getItem() == null) return;
                abrirMenu(getItem(), this, e.getScreenX(), e.getScreenY());
                e.consume();
            });
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
        Terminal.dialogo(alerta, alerta.getAlertType() == Alert.AlertType.ERROR ? "!! ERROR" : "EXPORTAR CSV",
                "boton-accion", null);
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

    public void setAlEliminar(Consumer<List<T>> accion) { this.alEliminar = accion; }
    public void setAlCambiarEstado(BiConsumer<T, Transicion> accion) { this.alCambiarEstado = accion; }

    public void setAlVerDetalle(Consumer<T> accion) {
        this.alVerDetalle = accion;
    }

    public void setAlFavorito(Consumer<T> accion) {
        this.alFavorito = accion;
    }

    public void setAlFavoritosEnLote(Consumer<List<T>> accion) {
        this.alFavoritosEnLote = accion;
    }

    public void agregarColumna(String id, String titulo, Function<T, Object> extractor, boolean visiblePorDefecto) {
        tabla.getColumns().add(tabla.getColumns().size() - COMUNES, columna(id, titulo, extractor));
        if (!visiblePorDefecto) ocultasPorDefecto.add(id);
    }

    private TableColumn<T, Object> columna(String id, String titulo, Function<T, Object> extractor) {
        TableColumn<T, Object> c = new TableColumn<>(titulo);
        c.setId(id);
        c.getStyleClass().add("col-" + id);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(extractor.apply(d.getValue())));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setText(null);
                } else if (valor instanceof Double decimal) {
                    setText(Formato.decimal(decimal));
                } else {
                    setText(valor.toString());
                }
            }
        });
        c.sortTypeProperty().addListener((obs, antes, ahora) -> guardarOrden());
        return c;
    }

    private static FechaCelda celdaInicio(ItemConsumption i) {
        if (i.isSinFechaInicio()) return new FechaCelda(null, "SIN REGISTRO");
        LocalDate f = i.getFechaInicio();
        return new FechaCelda(f, f == null ? "-" : f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private static FechaCelda celdaTermino(ItemConsumption i) {
        if (i.isSinFechaTermino()) return new FechaCelda(null, "SIN REGISTRO");
        LocalDate f = i.getFechaTermino();
        if (f != null) return new FechaCelda(f, f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        return new FechaCelda(null, i.getEstado() == Estado.EN_PROGRESO ? "EN CURSO" : "-");
    }

    private void ordenarPorFecha(TableColumn<T, Object> columna) {
        columna.setComparator((a, b) -> {
            LocalDate x = ((FechaCelda) a).fecha();
            LocalDate y = ((FechaCelda) b).fecha();
            if (x == null || y == null) {
                if (x == null && y == null) return 0;
                int resultado = (x == null) ? 1 : -1;
                return columna.getSortType() == TableColumn.SortType.DESCENDING ? -resultado : resultado;
            }
            return x.compareTo(y);
        });
    }
}