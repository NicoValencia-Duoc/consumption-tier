package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class PaginaInicio extends VBox {

    private static final String[] FILTROS = {"TODO", "JUEGOS", "LIBROS", "PELICULAS", "FAVORITOS", "SERIES"};

    private final List<ItemConsumption> items;
    private final Consumer<ItemConsumption> alAbrir;
    private final TilePane galeria = new TilePane(14, 14);
    private final TextField buscador = new TextField();
    private final ComboBox<String> orden = new ComboBox<>();
    private final Label subtitulo = new Label();
    private final List<Button> botonesFiltro = new ArrayList<>();
    private final Consumer<String> alAgregarTipo;
    private final ScrollPane scroll;
    private final VBox bienvenida;
    private final Label sinResultados = new Label("NADA QUE MOSTRAR CON ESTE FILTRO");
    private String filtroActual = "TODO";

    public PaginaInicio(List<ItemConsumption> items, Consumer<ItemConsumption> alAbrir,
                        Consumer<String> alAgregarTipo) {
        this.items = items;
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
        getChildren().addAll(crearBarraHerramientas(), crearFilaOrden(), zona);
        actualizar();
    }

    private VBox crearBienvenida() {
        Label titulo = new Label("THE MAW TIENE HAMBRE");
        titulo.getStyleClass().add("titulo-detalle");
        Label texto = new Label("AUN NO HAS DEVORADO NADA. SACIALA CON TU PRIMER REGISTRO.");
        texto.getStyleClass().add("subtitulo");

        HBox botones = new HBox(8);
        botones.setAlignment(Pos.CENTER);
        for (String tipo : new String[] {"JUEGO", "LIBRO", "PELICULA", "SERIE"}) {
            Button b = new Button("+ " + tipo);
            b.getStyleClass().addAll("pestana", "boton-accion");
            b.setOnAction(e -> alAgregarTipo.accept(tipo));
            botones.getChildren().add(b);
        }

        VBox caja = new VBox(14, titulo, texto, botones);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    private HBox crearBarraHerramientas() {
        buscador.setPromptText("BUSCAR POR TITULO...");
        buscador.getStyleClass().add("buscador");
        buscador.textProperty().addListener((obs, antes, ahora) -> actualizar());
        HBox.setHgrow(buscador, Priority.ALWAYS);

        HBox filtros = new HBox(6);
        for (String nombre : FILTROS) {
            Button b = new Button(nombre);
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
        return barra;
    }

    private HBox crearFilaOrden() {
        orden.getItems().addAll("NOTA ▼", "NOTA ▲", "TITULO A-Z", "TERMINADOS PRIMERO");
        orden.setValue("NOTA ▼");
        orden.setOnAction(e -> actualizar());

        Label etiqueta = new Label("ORDENAR POR");
        etiqueta.getStyleClass().add("subtitulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox fila = new HBox(10, subtitulo, espacio, etiqueta, orden);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private void actualizar() {
        for (Button b : botonesFiltro) {
            b.getStyleClass().remove("filtro-activo");
            if (b.getText().equals(filtroActual)) b.getStyleClass().add("filtro-activo");
        }
        String texto = buscador.getText().trim().toLowerCase();

        List<ItemConsumption> visibles = items.stream()
                .filter(i -> i.getEstado() != Estado.PENDIENTE)   // los pendientes tienen su propia pestana
                .filter(this::coincideConFiltro)
                .filter(i -> texto.isEmpty() || i.getTitulo().toLowerCase().contains(texto))
                .sorted(comparador())
                .toList();

        galeria.getChildren().clear();
        for (ItemConsumption i : visibles) {
            TarjetaItem tarjeta = new TarjetaItem(i);
            tarjeta.setCursor(Cursor.HAND);
            tarjeta.setOnMouseClicked(e -> alAbrir.accept(i));
            galeria.getChildren().add(tarjeta);
        }
        subtitulo.setText(visibles.size() + " ITEMS · ORDENADOS POR " + orden.getValue());

        boolean sinNada = items.isEmpty();
        bienvenida.setVisible(sinNada);
        scroll.setVisible(!sinNada);
        sinResultados.setVisible(!sinNada && visibles.isEmpty());
    }

    private boolean coincideConFiltro(ItemConsumption i) {
        return switch (filtroActual) {
            case "JUEGOS", "LIBROS", "PELICULAS", "SERIES" -> i.getTipo().equals(filtroActual);
            case "FAVORITOS" -> i.isFavorito();
            default -> true;
        };
    }

    private Comparator<ItemConsumption> comparador() {
        Comparator<ItemConsumption> porNotaDesc = Comparator.comparingDouble(ItemConsumption::getNota).reversed();
        return switch (orden.getValue()) {
            case "NOTA ▲" -> Comparator.comparingDouble(ItemConsumption::getNota);
            case "TITULO A-Z" -> Comparator.comparing(ItemConsumption::getTitulo, String.CASE_INSENSITIVE_ORDER);
            case "TERMINADOS PRIMERO" -> Comparator.<ItemConsumption, Boolean>comparing(
                    i -> i.getEstado() != Estado.TERMINADO).thenComparing(porNotaDesc);
            default -> porNotaDesc;
        };
    }
}