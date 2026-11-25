package consumption.ui;

import consumption.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.*;

import java.util.List;

public class PaginaEstadisticas extends VBox {

    public PaginaEstadisticas(List<ItemConsumption> items) {
        setSpacing(18);

        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        List<Pelicula> peliculas = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();
        List<Serie> series = items.stream().filter(Serie.class::isInstance).map(Serie.class::cast).toList();

        long favoritos = items.stream().filter(ItemConsumption::isFavorito).count();
        Label general = new Label("// " + items.size() + " ITEMS EN TOTAL · " + favoritos + " FAVORITOS");
        general.getStyleClass().add("subtitulo");

        double horasJuego = juegos.stream().mapToDouble(Juego::getHorasJugadas).sum();
        int paginas = libros.stream().filter(l -> l.getEstado() == Estado.TERMINADO)
                .mapToInt(Libro::getPaginas).sum();
        double horasPeliculas = peliculas.stream().filter(p -> p.getEstado() == Estado.TERMINADO)
                .mapToInt(Pelicula::getDuracionMin).sum() / 60.0;
        int episodios = series.stream().filter(s -> s.getEstado() == Estado.TERMINADO)
                .mapToInt(Serie::getEpisodios).sum();

        GridPane paneles = new GridPane();
        paneles.setHgap(14);
        paneles.setVgap(14);
        ColumnConstraints mitad = new ColumnConstraints();
        mitad.setPercentWidth(50);
        mitad.setMinWidth(0);
        paneles.getColumnConstraints().addAll(mitad, mitad);
        paneles.add(panel(Estadisticas.resumir("JUEGOS", juegos), "HORAS JUGADAS", Formato.decimal(horasJuego)), 0, 0);
        paneles.add(panel(Estadisticas.resumir("LIBROS", libros), "PAGINAS (TERMINADOS)", String.valueOf(paginas)), 1, 0);
        paneles.add(panel(Estadisticas.resumir("PELICULAS", peliculas), "HORAS (TERMINADAS)", Formato.decimal(horasPeliculas)), 0, 1);
        paneles.add(panel(Estadisticas.resumir("SERIES", series), "EPISODIOS (TERMINADAS)", String.valueOf(episodios)), 1, 1);

        VBox cuerpo = new VBox(18, general, paneles);
        cuerpo.setPadding(new Insets(0, 8, 0, 0));

        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private VBox panel(Estadisticas.Resumen r, String etiquetaExtra, String valorExtra) {
        Label nombre = new Label("> " + r.nombre());
        nombre.getStyleClass().add("titulo-item");

        String[][] filas = {
                {"ITEMS", String.valueOf(r.total())},
                {"TERMINADOS", String.valueOf(r.terminados())},
                {"EN PROGRESO", String.valueOf(r.enProgreso())},
                {"ABANDONADOS", String.valueOf(r.abandonados())},
                {"PENDIENTES", String.valueOf(r.pendientes())},
                {"NOTA PROMEDIO", r.notaPromedio() > 0 ? Formato.decimal(r.notaPromedio()) : "-"},
                {"MEJOR VALORADO", r.mejor()},
                {etiquetaExtra, valorExtra}};

        GridPane datos = new GridPane();
        datos.setHgap(20);
        datos.setVgap(6);
        for (int i = 0; i < filas.length; i++) {
            Label etiqueta = new Label(filas[i][0]);
            etiqueta.getStyleClass().add("subtitulo");
            etiqueta.setMinWidth(Region.USE_PREF_SIZE);
            Label valor = new Label(filas[i][1]);
            valor.getStyleClass().add("valor-criterio");
            valor.setWrapText(false);
            valor.setMinWidth(Region.USE_PREF_SIZE);
            datos.add(etiqueta, 0, i);
            datos.add(valor, 1, i);
        }

        ScrollPane desplazamiento = new ScrollPane(datos);
        desplazamiento.setFitToWidth(false);
        desplazamiento.setFitToHeight(false);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setMinWidth(0);
        desplazamiento.setMaxWidth(Double.MAX_VALUE);
        desplazamiento.getStyleClass().add("estadisticas-desplazamiento");

        desplazamiento.setPrefViewportHeight(280);
        desplazamiento.setMinHeight(Region.USE_PREF_SIZE);
        desplazamiento.setMaxHeight(Region.USE_PREF_SIZE);

        VBox.setVgrow(desplazamiento, Priority.NEVER);

        ScrollBar barraHorizontal = new ScrollBar();
        barraHorizontal.setOrientation(Orientation.HORIZONTAL);
        barraHorizontal.setMin(0);
        barraHorizontal.setMax(1);
        barraHorizontal.valueProperty().bindBidirectional(desplazamiento.hvalueProperty());
        barraHorizontal.getStyleClass().add("estadisticas-barra-horizontal");

        javafx.beans.InvalidationListener ajustarBarra = obs -> {
            double anchoContenido = datos.getLayoutBounds().getWidth();
            double anchoVisible = desplazamiento.getViewportBounds().getWidth();
            if (anchoContenido <= 0 || anchoVisible <= 0) return;

            double proporcion = Math.min(1.0, anchoVisible / anchoContenido);
            barraHorizontal.setVisibleAmount(proporcion);

            double recorrido = Math.max(1, anchoContenido - anchoVisible);
            barraHorizontal.setUnitIncrement(Math.min(0.05, 16.0 / recorrido));
            barraHorizontal.setBlockIncrement(Math.min(0.25, anchoVisible * 0.8 / recorrido));
            barraHorizontal.setDisable(anchoContenido <= anchoVisible + 1);
        };
        datos.layoutBoundsProperty().addListener(ajustarBarra);
        desplazamiento.viewportBoundsProperty().addListener(ajustarBarra);
        javafx.application.Platform.runLater(() -> ajustarBarra.invalidated(null));

        VBox caja = new VBox(12, nombre, desplazamiento, barraHorizontal);
        caja.getStyleClass().add("tarjeta");
        caja.setMinWidth(0);
        caja.setMaxWidth(Double.MAX_VALUE);
        caja.setMinHeight(Region.USE_PREF_SIZE);
        return caja;
    }
}