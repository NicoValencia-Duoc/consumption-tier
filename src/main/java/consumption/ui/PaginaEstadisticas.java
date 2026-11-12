package consumption.ui;

import consumption.model.*;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;

public class PaginaEstadisticas extends VBox {

    public PaginaEstadisticas(List<ItemConsumption> items, Runnable alRespaldar, Runnable alRestaurar) {
        setSpacing(18);

        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        List<Pelicula> peliculas = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();

        long favoritos = items.stream().filter(ItemConsumption::isFavorito).count();
        Label general = new Label(items.size() + " ITEMS EN TOTAL · " + favoritos + " FAVORITOS");
        general.getStyleClass().add("subtitulo");

        double horasJuego = juegos.stream().mapToDouble(Juego::getHorasJugadas).sum();
        int paginas = libros.stream().filter(l -> l.getEstado() == Estado.TERMINADO)
                .mapToInt(Libro::getPaginas).sum();
        double horasPeliculas = peliculas.stream().filter(p -> p.getEstado() == Estado.TERMINADO)
                .mapToInt(Pelicula::getDuracionMin).sum() / 60.0;

        HBox paneles = new HBox(14,
                panel(Estadisticas.resumir("JUEGOS", juegos), "HORAS JUGADAS", String.format("%.1f", horasJuego)),
                panel(Estadisticas.resumir("LIBROS", libros), "PAGINAS (TERMINADOS)", String.valueOf(paginas)),
                panel(Estadisticas.resumir("PELICULAS", peliculas), "HORAS (TERMINADAS)", String.format("%.1f", horasPeliculas)));

        Label herramientas = new Label("RESPALDO DE TUS DATOS (INCLUYE LAS IMAGENES)");
        herramientas.getStyleClass().add("subtitulo");

        Button respaldar = new Button("CREAR RESPALDO");
        respaldar.getStyleClass().addAll("pestana", "boton-accion");
        respaldar.setOnAction(e -> alRespaldar.run());

        Button restaurar = new Button("RESTAURAR RESPALDO");
        restaurar.getStyleClass().addAll("pestana", "boton-peligro");
        restaurar.setOnAction(e -> alRestaurar.run());

        HBox botones = new HBox(8, respaldar, restaurar);
        botones.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(general, paneles, herramientas, botones);
    }

    private VBox panel(Estadisticas.Resumen r, String etiquetaExtra, String valorExtra) {
        Label nombre = new Label(r.nombre());
        nombre.getStyleClass().add("titulo-item");

        String[][] filas = {
                {"ITEMS", String.valueOf(r.total())},
                {"TERMINADOS", String.valueOf(r.terminados())},
                {"EN PROGRESO", String.valueOf(r.enProgreso())},
                {"ABANDONADOS", String.valueOf(r.abandonados())},
                {"PENDIENTES", String.valueOf(r.pendientes())},
                {"NOTA PROMEDIO", r.notaPromedio() > 0 ? String.format("%.1f", r.notaPromedio()) : "-"},
                {"MEJOR VALORADO", r.mejor()},
                {etiquetaExtra, valorExtra}};

        GridPane datos = new GridPane();
        datos.setHgap(20);
        datos.setVgap(6);
        for (int i = 0; i < filas.length; i++) {
            Label etiqueta = new Label(filas[i][0]);
            etiqueta.getStyleClass().add("subtitulo");
            Label valor = new Label(filas[i][1]);
            valor.getStyleClass().add("valor-criterio");
            valor.setWrapText(true);
            datos.add(etiqueta, 0, i);
            datos.add(valor, 1, i);
        }

        VBox caja = new VBox(12, nombre, datos);
        caja.getStyleClass().add("tarjeta");
        caja.setPrefWidth(330);
        return caja;
    }
}