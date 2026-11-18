package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;
import consumption.model.ReglasInicio;
import consumption.model.Tier;
import consumption.model.Transicion;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PaginaInicio extends VBox {

    private static final String[] FILTROS =
            {"TODO", "JUEGOS", "LIBROS", "PELICULAS", "SERIES", "FAVORITOS", "SIN DEVORAR"};

    private final boolean tarjetasCompactas;
    private final List<ItemConsumption> items;
    private final Consumer<ItemConsumption> alAbrir;
    private final TilePane galeria = new TilePane(14, 14);
    private final CampoBusqueda buscador = new CampoBusqueda();
    private final ComboBox<String> orden = new ComboBox<>();
    private final Label subtitulo = new Label();
    private final List<Button> botonesFiltro = new ArrayList<>();
    private final Map<Tier, Button> chipsTier = new EnumMap<>(Tier.class);
    private final Consumer<String> alAgregarTipo;

    private ContextMenu menuAbierto;
    private Consumer<ItemConsumption> alEditar = i -> {};
    private Consumer<ItemConsumption> alFavorito = i -> {};
    private Consumer<List<ItemConsumption>> alEliminar = lote -> {};
    private BiConsumer<ItemConsumption, Transicion> alCambiarEstado = (i, transicion) -> {};
    private final ScrollPane scroll;
    private final VBox bienvenida;
    private final Label sinResultados = new Label("( ◉ ) NADA RESPONDE A ESE FILTRO");
    private String filtroActual = "TODO";
    private Tier tierActual = null;

    public PaginaInicio(List<ItemConsumption> items, Consumer<ItemConsumption> alAbrir,
                        Consumer<String> alAgregarTipo, boolean tarjetasCompactas) {
        this.items = items;
        this.tarjetasCompactas = tarjetasCompactas;
        this.alAbrir = alAbrir;
        this.alAgregarTipo = alAgregarTipo;
        setSpacing(14);

        galeria.setPrefColumns(4);
        scroll = new ScrollPane(galeria);
        scroll.setFitToWidth(true);

        bienvenida = crearBienvenida();
        sinResultados.getStyleClass().add("subtitulo");

        StackPane zona = new StackPane(scroll, bienvenida, sinResultados);
        VBox.setVgrow(zona, Priority.ALWAYS);

        subtitulo.getStyleClass().add("subtitulo");
        getChildren().addAll(new VBox(2, Terminal.titulo("BIBLIOTECA", null), subtitulo),
                crearBarraHerramientas(), crearFilaTier(), zona);
        actualizar();
    }

    private VBox crearBienvenida() {
        Label titulo = new Label("( ◉ )");
        titulo.getStyleClass().add("ojo-maw");
        Label texto = new Label("ALGO RESPIRA AQUI.");
        texto.getStyleClass().add("vacio-titulo");
        Label detalle = new Label("AUN NO HAS ALIMENTADO A THE MAW. ELIGE QUE DARLE DE COMER.");
        detalle.getStyleClass().add("subtitulo");

        HBox botones = new HBox(8);
        botones.setAlignment(Pos.CENTER);
        for (String tipo : new String[] {"JUEGO", "LIBRO", "PELICULA", "SERIE"}) {
            Button b = Terminal.corchetes(new Button("+ " + tipo));
            b.getStyleClass().addAll("pestana", "boton-accion");
            b.setOnAction(e -> alAgregarTipo.accept(tipo));
            botones.getChildren().add(b);
        }

        VBox caja = new VBox(14, titulo, texto, detalle, botones);
        caja.getStyleClass().add("vacio-caja");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return caja;
    }

    private VBox crearBarraHerramientas() {
        buscador.setPromptText("buscar --titulo --autor --genero");
        buscador.textProperty().addListener((obs, antes, ahora) -> actualizar());
        HBox.setHgrow(buscador, Priority.ALWAYS);

        HBox filtros = new HBox(6);
        for (String nombre : FILTROS) {
            Button b = new Button(nombre);
            b.setUserData(nombre);
            Terminal.corchetes(b);
            b.getStyleClass().add("filtro");
            b.setOnAction(e -> {
                filtroActual = nombre;
                actualizar();
            });
            botonesFiltro.add(b);
            filtros.getChildren().add(b);
        }

        HBox barra = new HBox(12, buscador, filtros);
        barra.setAlignment(Pos.CENTER_LEFT);

        buscador.setMinWidth(280);

        VBox contenedor = new VBox(8, barra);

        contenedor.widthProperty().addListener((obs, anterior, ancho) -> {
            boolean estrecho = ancho.doubleValue() < 1050;

            if (estrecho && filtros.getParent() == barra) {
                barra.getChildren().remove(filtros);
                contenedor.getChildren().add(filtros);
            } else if (!estrecho && filtros.getParent() == contenedor) {
                contenedor.getChildren().remove(filtros);
                barra.getChildren().add(filtros);
            }
        });

        return contenedor;
    }

    private HBox crearFilaTier() {
        Label etiqueta = new Label("TIER");
        etiqueta.getStyleClass().add("subtitulo");

        HBox fila = new HBox(6, etiqueta);
        fila.setAlignment(Pos.CENTER_LEFT);
        for (Tier t : Tier.values()) {
            Button chip = new Button(t.getLetra());
            chip.getStyleClass().addAll("chip-tier", "chip-" + t.getLetra().toLowerCase());
            chip.setOnAction(e -> {
                tierActual = (tierActual == t) ? null : t;
                actualizar();
            });
            chipsTier.put(t, chip);
            fila.getChildren().add(chip);
        }

        orden.getItems().addAll("NOTA ▼", "NOTA ▲", "TITULO A-Z", "TERMINADOS PRIMERO");
        orden.setValue("NOTA ▼");
        orden.setOnAction(e -> actualizar());
        Label ordenar = new Label("ORDENAR");
        ordenar.getStyleClass().add("subtitulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        fila.getChildren().addAll(espacio, ordenar, orden);
        return fila;
    }
    private void actualizar() {
        for (Button b : botonesFiltro) {
            b.getStyleClass().remove("filtro-activo");
            if (filtroActual.equals(b.getUserData())) b.getStyleClass().add("filtro-activo");
        }
        for (Map.Entry<Tier, Button> e : chipsTier.entrySet()) {
            e.getValue().getStyleClass().remove("chip-activo");
            if (e.getKey() == tierActual) e.getValue().getStyleClass().add("chip-activo");
        }
        String consulta = buscador.getText();

        List<ItemConsumption> visibles = items.stream()
                .filter(this::coincideConFiltro)
                .filter(i -> ReglasInicio.pasaTier(i, tierActual))
                .filter(i -> ReglasInicio.coincideBusqueda(i, consulta))
                .sorted(ReglasInicio.pendientesAlFinal(comparador()))
                .toList();

        galeria.getChildren().clear();
        for (ItemConsumption i : visibles) {
            TarjetaItem tarjeta = new TarjetaItem(i, tarjetasCompactas);
            tarjeta.setCursor(Cursor.HAND);
            tarjeta.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) alAbrir.accept(i);
            });
            tarjeta.setOnContextMenuRequested(e -> {
                if (menuAbierto != null) menuAbierto.hide();
                menuAbierto = new MenuAcciones<>(List.of(i), alEditar,
                        lote -> {
                            lote.forEach(alFavorito);
                            actualizar();
                        },
                        alCambiarEstado, alEliminar);
                menuAbierto.show(tarjeta, e.getScreenX(), e.getScreenY());
                e.consume();
            });
            galeria.getChildren().add(tarjeta);
        }

        long pendientes = visibles.stream().filter(i -> i.getEstado() == Estado.PENDIENTE).count();
        String texto = visibles.size() + (visibles.size() == 1 ? " ITEM" : " ITEMS");
        if (pendientes > 0) texto += " · " + pendientes + " SIN DEVORAR";
        subtitulo.setText("// " + texto);

        boolean sinNada = items.isEmpty();
        bienvenida.setVisible(sinNada);
        scroll.setVisible(!sinNada);
        sinResultados.setVisible(!sinNada && visibles.isEmpty());
    }

    private boolean coincideConFiltro(ItemConsumption i) {
        return switch (filtroActual) {
            case "JUEGOS", "LIBROS", "PELICULAS", "SERIES" -> i.getTipo().equals(filtroActual);
            case "FAVORITOS" -> i.isFavorito();
            case "SIN DEVORAR" -> i.getEstado() == Estado.PENDIENTE;
            default -> true;
        };
    }

    public void setAlEditar(Consumer<ItemConsumption> accion) {
        this.alEditar = accion;
    }

    public void setAlFavorito(Consumer<ItemConsumption> accion) {
        this.alFavorito = accion;
    }

    public void setAlEliminar(Consumer<List<ItemConsumption>> accion) {
        this.alEliminar = accion;
    }

    public void setAlCambiarEstado(BiConsumer<ItemConsumption, Transicion> accion) {
        this.alCambiarEstado = accion;
    }

    private Comparator<ItemConsumption> comparador() {
        Comparator<ItemConsumption> alfabetico =
                Comparator.comparing(ItemConsumption::getTitulo, String.CASE_INSENSITIVE_ORDER);
        Comparator<ItemConsumption> porNotaDesc =
                Comparator.comparingDouble(ItemConsumption::getNota).reversed().thenComparing(alfabetico);
        return switch (orden.getValue()) {
            case "NOTA ▲" -> Comparator.comparingDouble(ItemConsumption::getNota).thenComparing(alfabetico);
            case "TITULO A-Z" -> alfabetico;
            case "TERMINADOS PRIMERO" -> Comparator.<ItemConsumption, Boolean>comparing(
                    i -> i.getEstado() != Estado.TERMINADO).thenComparing(porNotaDesc);
            default -> porNotaDesc;
        };
    }
}