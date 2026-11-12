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
    private String filtroActual = "TODO";

    public PaginaInicio(List<ItemConsumption> items, Consumer<ItemConsumption> alAbrir) {
        this.items = items;
        this.alAbrir = alAbrir;
        setSpacing(14);

        galeria.setPrefColumns(4);
        ScrollPane scroll = new ScrollPane(galeria);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        subtitulo.getStyleClass().add("subtitulo");
        getChildren().addAll(crearBarraHerramientas(), crearFilaOrden(), scroll);
        actualizar();
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