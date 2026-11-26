package consumption.ui;

import consumption.model.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaginaEstadisticas extends VBox {
    private static final int SEGMENTOS_TIER = 20;
    private static final int SEGMENTOS_RANKING = 20;
    private static final int FILAS_MINI = 6;
    private static final int TOPE_GENEROS = 10;
    private static final int MAX_ANIOS = 8;
    private static final double ALTO_BARRA_TIER = 190;
    private static final double ALTO_BARRA_MINI = 56;
    private static final double RELLENO_DERECHO = 8;
    private static final double SEPARACION = ReglasEstadisticas.SEPARACION;

    private final int escalaTexto;
    private final double factor;
    private final VBox cuerpo = new VBox(18);
    private final List<Pane> contenedores = new ArrayList<>();
    private final Label general;
    private final Node panelTiers;
    private final Node panelRanking;
    private final Node panelMeses;
    private final Node panelAnios;
    private final List<Node> tarjetas;
    private ReglasEstadisticas.Modo modoActual;

    public PaginaEstadisticas(List<ItemConsumption> items, int escalaTexto) {
        setSpacing(18);
        this.escalaTexto = escalaTexto;
        this.factor = ReglasEstadisticas.factorEscala(escalaTexto);

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

        this.general = general;
        LocalDate hoy = LocalDate.now();
        GraficosEstadisticas.Distribucion tiers = GraficosEstadisticas.distribucionTiers(items);
        List<GraficosEstadisticas.Barra> anios = GraficosEstadisticas.porAnio(items, MAX_ANIOS);
        panelTiers = panel("DISTRIBUCION DE TIERS", "% DE LOS " + tiers.total() + " ITEMS CON NOTA",
                construirTiers(tiers));
        panelRanking = panel("RANKING POR GENERO", "TOP " + TOPE_GENEROS,
                construirRanking(GraficosEstadisticas.rankingGeneros(items, TOPE_GENEROS)));
        panelMeses = panel("DEVORADOS POR MES", "ULTIMOS 12",
                construirMini(GraficosEstadisticas.porMes(items, hoy), 11));
        panelAnios = panel("ITEMS POR AÑO", "TERMINO",
                anios.isEmpty() ? textoVacio() : construirMini(anios, anios.size() - 1));
        tarjetas = List.of(
                panel("JUEGOS", null, filasResumen(Estadisticas.resumir("JUEGOS", juegos),
                        "HORAS JUGADAS", Formato.decimal(horasJuego))),
                panel("LIBROS", null, filasResumen(Estadisticas.resumir("LIBROS", libros),
                        "PAGINAS (TERMINADOS)", String.valueOf(paginas))),
                panel("PELICULAS", null, filasResumen(Estadisticas.resumir("PELICULAS", peliculas),
                        "HORAS (TERMINADAS)", Formato.decimal(horasPeliculas))),
                panel("SERIES", null, filasResumen(Estadisticas.resumir("SERIES", series),
                        "EPISODIOS (TERMINADOS)", String.valueOf(episodios))));

        cuerpo.setPadding(new Insets(0, RELLENO_DERECHO, 0, 0));
        cuerpo.getChildren().add(general);

        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scroll.viewportBoundsProperty().addListener((obs, antes, ahora) -> {
            if (ahora.getWidth() > 0) ajustar(ahora.getWidth() - RELLENO_DERECHO);
        });
        getChildren().add(scroll);
    }

    private void ajustar(double ancho) {
        ReglasEstadisticas.Modo modo = ReglasEstadisticas.modo(ancho, escalaTexto);
        if (modo.equals(modoActual)) return;
        modoActual = modo;
        for (Pane contenedor : contenedores) contenedor.getChildren().clear();
        contenedores.clear();
        cuerpo.getChildren().setAll(general, superior(modo), tarjetasEnFilas(modo));
    }

    private <T extends Pane> T registrar(T contenedor) {
        contenedores.add(contenedor);
        return contenedor;
    }

    private Node superior(ReglasEstadisticas.Modo modo) {
        if (!modo.dosSuperiores()) {
            return registrar(new VBox(SEPARACION, panelTiers, panelRanking, minis(modo)));
        }
        VBox izquierda = registrar(new VBox(SEPARACION, panelTiers, minis(modo)));
        GridPane fila = registrar(new GridPane());
        fila.setHgap(SEPARACION);
        fila.getColumnConstraints().addAll(columnaPorcentual(58), columnaPorcentual(42));
        fila.add(izquierda, 0, 0);
        fila.add(panelRanking, 1, 0);
        return fila;
    }

    private Node minis(ReglasEstadisticas.Modo modo) {
        if (modo.columnasMini() < 2) {
            return registrar(new VBox(SEPARACION, panelMeses, panelAnios));
        }
        GridPane fila = registrar(new GridPane());
        fila.setHgap(SEPARACION);
        fila.getColumnConstraints().addAll(columnaPorcentual(50), columnaPorcentual(50));
        fila.add(panelMeses, 0, 0);
        fila.add(panelAnios, 1, 0);
        return fila;
    }

    private Node tarjetasEnFilas(ReglasEstadisticas.Modo modo) {
        GridPane rejilla = registrar(new GridPane());
        rejilla.setHgap(SEPARACION);
        rejilla.setVgap(SEPARACION);
        int n = modo.columnasTarjetas();
        for (int c = 0; c < n; c++) rejilla.getColumnConstraints().add(columnaPorcentual(100.0 / n));
        for (int i = 0; i < tarjetas.size(); i++) rejilla.add(tarjetas.get(i), i % n, i / n);
        return rejilla;
    }

    private static ColumnConstraints columnaPorcentual(double porcentaje) {
        ColumnConstraints columna = new ColumnConstraints();
        columna.setPercentWidth(porcentaje);
        columna.setMinWidth(0);
        return columna;
    }

    private StackPane panel(String titulo, String subtitulo, Node contenido) {
        Label nombre = new Label("> " + titulo);
        nombre.getStyleClass().add("titulo-item");
        FlowPane cabecera = new FlowPane(8, 2);
        cabecera.getChildren().add(nombre);
        if (subtitulo != null) {
            Label detalle = new Label(subtitulo);
            detalle.getStyleClass().add("subtitulo");
            cabecera.getChildren().add(detalle);
        }
        VBox interior = new VBox(12, cabecera, contenido);
        interior.getStyleClass().add("panel-estadistica-cuerpo");
        Region arriba = new Region();
        arriba.getStyleClass().addAll("panel-esquina", "panel-esquina-sup");
        Region abajo = new Region();
        abajo.getStyleClass().addAll("panel-esquina", "panel-esquina-inf");
        arriba.setMouseTransparent(true);
        abajo.setMouseTransparent(true);
        StackPane.setAlignment(arriba, Pos.TOP_RIGHT);
        StackPane.setAlignment(abajo, Pos.BOTTOM_RIGHT);
        StackPane caja = new StackPane(interior, arriba, abajo);
        caja.getStyleClass().add("panel-estadistica");
        caja.setMinWidth(0);
        caja.setMaxWidth(Double.MAX_VALUE);
        caja.setMaxHeight(Double.MAX_VALUE);
        return caja;
    }

    private Label textoVacio() {
        Label vacio = new Label("SIN DATOS");
        vacio.getStyleClass().add("subtitulo");
        return vacio;
    }

    private static void fijarAlto(Region region, double alto) {
        region.setMinHeight(alto);
        region.setPrefHeight(alto);
        region.setMaxHeight(alto);
    }

    private Node construirTiers(GraficosEstadisticas.Distribucion distribucion) {
        GridPane rejilla = new GridPane();
        rejilla.setHgap(6);
        rejilla.setVgap(6);
        rejilla.getColumnConstraints().add(new ColumnConstraints());
        SegmentosLed.Eje eje = new SegmentosLed.Eje(new double[] {1.0, 0.75, 0.5, 0.25},
                new String[] {"100%", "75%", "50%", "25%"});
        fijarAlto(eje, ALTO_BARRA_TIER);
        rejilla.add(eje, 0, 1);
        int columna = 1;
        for (GraficosEstadisticas.BarraTier b : distribucion.barras()) {
            ColumnConstraints ancho = new ColumnConstraints();
            ancho.setHgrow(Priority.ALWAYS);
            ancho.setMinWidth(0);
            ancho.setPrefWidth(60);
            rejilla.getColumnConstraints().add(ancho);

            Label porcentaje = new Label(Formato.decimal(b.porcentaje()) + "%");
            porcentaje.getStyleClass().add("grafico-valor");
            Label cuenta = new Label("(" + b.cantidad() + ")");
            cuenta.getStyleClass().add("subtitulo");
            VBox cabecera = new VBox(0, porcentaje, cuenta);
            cabecera.setAlignment(Pos.CENTER);

            SegmentosLed barra = new SegmentosLed(true, SEGMENTOS_TIER,
                    ReglasEstadisticas.segmentosPorFraccion(b.porcentaje() / 100.0, SEGMENTOS_TIER),
                    b.tier().getClaseCss(), 5, 10, 15);
            fijarAlto(barra, ALTO_BARRA_TIER);
            EtiquetaTier letra = new EtiquetaTier(b.tier(), "tier-chico");

            rejilla.add(cabecera, columna, 0);
            rejilla.add(barra, columna, 1);
            rejilla.add(letra, columna, 2);
            GridPane.setHalignment(cabecera, HPos.CENTER);
            GridPane.setHalignment(letra, HPos.CENTER);
            GridPane.setMargin(barra, new Insets(0, 4, 0, 4));
            columna++;
        }
        return rejilla;
    }

    private Node construirRanking(List<GraficosEstadisticas.Barra> datos) {
        if (datos.isEmpty()) return textoVacio();
        int maximo = datos.get(0).valor();
        GridPane rejilla = new GridPane();
        rejilla.setHgap(12);
        rejilla.setVgap(10);
        ColumnConstraints nombres = new ColumnConstraints();
        nombres.setMaxWidth(190 * factor);
        ColumnConstraints barras = new ColumnConstraints();
        barras.setHgrow(Priority.ALWAYS);
        barras.setMinWidth(40);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHalignment(HPos.RIGHT);
        rejilla.getColumnConstraints().addAll(nombres, barras, valores);
        int fila = 0;
        for (GraficosEstadisticas.Barra b : datos) {
            Label etiqueta = new Label(b.etiqueta());
            etiqueta.getStyleClass().add("subtitulo");
            SegmentosLed led = new SegmentosLed(false, SEGMENTOS_RANKING,
                    ReglasEstadisticas.segmentosPorFraccion(b.valor() / (double) maximo, SEGMENTOS_RANKING), null);
            led.setMinHeight(12);
            led.setPrefHeight(12);
            led.setMaxHeight(12);
            led.setMaxWidth(Double.MAX_VALUE);
            Label cantidad = new Label(String.valueOf(b.valor()));
            cantidad.getStyleClass().add("grafico-valor");
            rejilla.add(etiqueta, 0, fila);
            rejilla.add(led, 1, fila);
            rejilla.add(cantidad, 2, fila);
            GridPane.setValignment(etiqueta, VPos.CENTER);
            GridPane.setValignment(led, VPos.CENTER);
            GridPane.setValignment(cantidad, VPos.CENTER);
            fila++;
        }
        return rejilla;
    }

    private Node construirMini(List<GraficosEstadisticas.Barra> datos, int resaltado) {
        int maximo = datos.stream().mapToInt(GraficosEstadisticas.Barra::valor).max().orElse(0);
        HBox fila = new HBox(3);
        for (int i = 0; i < datos.size(); i++) {
            GraficosEstadisticas.Barra b = datos.get(i);
            Label valor = new Label(String.valueOf(b.valor()));
            valor.getStyleClass().add("grafico-mini-valor");
            valor.setMinWidth(Region.USE_PREF_SIZE);
            int llenos = maximo <= 0 ? 0
                    : ReglasEstadisticas.segmentosPorFraccion(b.valor() / (double) maximo, FILAS_MINI);
            SegmentosLed led = new SegmentosLed(true, FILAS_MINI, llenos, i == resaltado ? "led-e-actual" : null);
            fijarAlto(led, ALTO_BARRA_MINI);
            Label etiqueta = new Label(b.etiqueta());
            etiqueta.getStyleClass().add("grafico-mini-etiqueta");
            etiqueta.setMinWidth(Region.USE_PREF_SIZE);
            VBox columna = new VBox(3, valor, led, etiqueta);
            columna.setAlignment(Pos.CENTER);
            columna.setPrefWidth(20);
            columna.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(columna, Priority.ALWAYS);
            fila.getChildren().add(columna);
        }
        return fila;
    }

    private Node filasResumen(Estadisticas.Resumen r, String etiquetaExtra, String valorExtra) {
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
        datos.setHgap(14);
        datos.setVgap(6);
        datos.getColumnConstraints().addAll(columnaPorcentual(46), columnaPorcentual(54));
        for (int i = 0; i < filas.length; i++) {
            Label etiqueta = new Label(filas[i][0]);
            etiqueta.getStyleClass().add("subtitulo");
            etiqueta.setWrapText(true);
            Label valor = new Label(filas[i][1]);
            valor.getStyleClass().add("grafico-valor");
            valor.setWrapText(true);
            datos.add(etiqueta, 0, i);
            datos.add(valor, 1, i);
            GridPane.setValignment(etiqueta, VPos.TOP);
            GridPane.setValignment(valor, VPos.TOP);
        }
        return datos;
    }
}