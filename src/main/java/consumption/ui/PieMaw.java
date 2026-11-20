package consumption.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class PieMaw extends VBox {

    private static final int SEGMENTOS = 10;
    private static final double ANCHO_BOTON = 40;
    private static final double ALTO_BOTON = 24;

    private final Label izquierda = new Label();
    private final Label nivel = new Label();
    private final Label porcentaje = new Label();
    private final HBox barra = new HBox(2);

    public PieMaw(ReproductorMusica reproductor) {
        super(0);
        getStyleClass().add("pie-maw");

        LineaProgreso linea = new LineaProgreso(reproductor.progresoProperty(), reproductor::buscar);

        Label punto = new Label("●");
        punto.getStyleClass().add("pie-punto");
        izquierda.getStyleClass().add("pie-texto");
        nivel.getStyleClass().add("pie-nivel");
        porcentaje.getStyleClass().add("pie-texto");
        Label etiquetaNivel = new Label("NIVEL");
        etiquetaNivel.getStyleClass().add("pie-texto");
        Label etiquetaHambre = new Label("HAMBRE");
        etiquetaHambre.getStyleClass().add("pie-texto");

        barra.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < SEGMENTOS; i++) {
            Region segmento = new Region();
            segmento.getStyleClass().add("pie-segmento");
            segmento.setMinSize(5, 10);
            segmento.setPrefSize(5, 10);
            segmento.setMaxSize(5, 10);
            barra.getChildren().add(segmento);
        }

        Label cancion = new Label();
        cancion.getStyleClass().add("cancion");
        cancion.setMinWidth(0);
        cancion.setMaxWidth(Double.MAX_VALUE);
        cancion.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        cancion.textProperty().bind(Bindings.concat("♪ ", reproductor.tituloProperty()));

        Label tiempo = new Label();
        tiempo.getStyleClass().add("tiempo");
        tiempo.setMinWidth(0);
        tiempo.setPrefWidth(100);
        tiempo.setMaxWidth(100);
        tiempo.textProperty().bind(reproductor.tiempoProperty());

        Button lista = new Button("☰");
        lista.getStyleClass().add("boton-musica");
        lista.setOnAction(e -> PistasDialog.mostrar(reproductor, getScene().getStylesheets()));
        fijarTamano(lista);

        Button pausa = new Button();
        pausa.getStyleClass().add("boton-musica");
        Node iconoPlay = crearIconoPlay();
        Node iconoPausa = crearIconoPausa();
        pausa.setGraphic(reproductor.sonandoProperty().get() ? iconoPausa : iconoPlay);
        reproductor.sonandoProperty().addListener((obs, antes, ahora) -> pausa.setGraphic(ahora ? iconoPausa : iconoPlay));
        pausa.setOnAction(e -> reproductor.alternarPausa());
        fijarTamano(pausa);

        Button siguiente = new Button("▶▶");
        siguiente.getStyleClass().add("boton-musica");
        siguiente.setOnAction(e -> reproductor.siguiente());
        fijarTamano(siguiente);

        Slider volumen = new Slider(0, 1, reproductor.volumenProperty().get());
        volumen.setMinWidth(90);
        volumen.setPrefWidth(90);
        volumen.setMaxWidth(90);
        reproductor.volumenProperty().bind(volumen.valueProperty());

        Region separador = new Region();
        separador.getStyleClass().add("separador-pestanas");
        separador.setMinSize(1, 16);
        separador.setPrefSize(1, 16);
        separador.setMaxSize(1, 16);

        HBox estado = new HBox(10, punto, izquierda);
        estado.setAlignment(Pos.CENTER_LEFT);
        estado.setMinWidth(0);
        estado.setMaxWidth(Double.MAX_VALUE);
        izquierda.setMinWidth(0);
        izquierda.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        HBox controles = new HBox(10, lista, pausa, siguiente, volumen);
        controles.setAlignment(Pos.CENTER_LEFT);
        controles.setMinWidth(Region.USE_PREF_SIZE);
        controles.setMaxWidth(Region.USE_PREF_SIZE);

        HBox hambre = new HBox(10, separador, etiquetaNivel, nivel, etiquetaHambre, barra, porcentaje);
        hambre.setAlignment(Pos.CENTER_LEFT);
        hambre.setMinWidth(Region.USE_PREF_SIZE);
        hambre.setMaxWidth(Region.USE_PREF_SIZE);
        porcentaje.setMinWidth(Region.USE_PREF_SIZE);

        StackPane tituloVisible = new StackPane(cancion);
        tituloVisible.setMinWidth(0);
        tituloVisible.setPrefWidth(220);
        tituloVisible.setMaxWidth(Double.MAX_VALUE);
        tituloVisible.setAlignment(Pos.CENTER_LEFT);

        Rectangle recorte = new Rectangle();
        tituloVisible.setClip(recorte);
        recorte.widthProperty().bind(tituloVisible.widthProperty());
        recorte.heightProperty().bind(tituloVisible.heightProperty());

        HBox centro = new HBox(10, tituloVisible, tiempo);
        centro.setAlignment(Pos.CENTER_LEFT);
        centro.setMinWidth(0);
        centro.setPrefWidth(250);
        centro.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tituloVisible, Priority.ALWAYS);
        HBox.setHgrow(centro, Priority.ALWAYS);
        cancion.setVisible(true);
        cancion.setManaged(true);
        tiempo.setVisible(true);
        tiempo.setManaged(true);

        HBox fila = new HBox(10);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("pie-fila");
        fila.setPadding(new Insets(0, 12, 0, 0));
        fila.setMinWidth(0);
        HBox.setHgrow(estado, Priority.ALWAYS);

        HBox filaMusica = new HBox(10);
        filaMusica.setAlignment(Pos.CENTER_LEFT);
        filaMusica.getStyleClass().add("pie-fila");
        filaMusica.setPadding(new Insets(0, 12, 0, 0));
        filaMusica.setMinWidth(0);

        HBox filaHambre = new HBox(10);
        filaHambre.setAlignment(Pos.CENTER_LEFT);
        filaHambre.getStyleClass().add("pie-fila");
        filaHambre.setPadding(new Insets(0, 12, 0, 0));
        filaHambre.setMinWidth(0);

        HBox filaExtra = new HBox(10);
        filaExtra.setAlignment(Pos.CENTER_LEFT);
        filaExtra.getStyleClass().add("pie-fila");
        filaExtra.setPadding(new Insets(0, 12, 0, 0));
        getChildren().addAll(linea, fila, filaMusica, filaHambre, filaExtra);
        widthProperty().addListener((obs, anterior, ancho) ->
                distribuirPie(ancho.doubleValue(), fila, filaMusica, filaHambre, filaExtra,
                        estado, centro, controles, hambre, izquierda));
        distribuirPie(1200, fila, filaMusica, filaHambre, filaExtra,
                estado, centro, controles, hambre, izquierda);

        if (!reproductor.hayMusica()) {
            linea.setDisable(true);
            lista.setDisable(true);
            pausa.setDisable(true);
            siguiente.setDisable(true);
            volumen.setDisable(true);
        }
    }

    private int distribucionActual = -1;

    private void distribuirPie(double ancho, HBox fila, HBox filaMusica, HBox filaHambre,
                               HBox filaExtra, HBox estado, HBox centro, HBox controles, HBox hambre,
                               Label textoEstado) {
        int modo = ancho >= 1650 ? 0 : ancho >= 950 ? 1 : ancho >= 680 ? 2 : 3;
        if (modo == distribucionActual) return;
        distribucionActual = modo;
        fila.getChildren().clear();
        filaMusica.getChildren().clear();
        filaHambre.getChildren().clear();
        filaExtra.getChildren().clear();
        if (modo == 0) {
            textoEstado.setWrapText(false);
            fila.getChildren().addAll(estado, centro, controles, hambre);
        } else if (modo == 1) {
            textoEstado.setWrapText(true);
            fila.getChildren().add(estado);
            filaMusica.getChildren().addAll(centro, controles, hambre);
        } else {
            textoEstado.setWrapText(true);
            fila.getChildren().add(estado);
            if (modo == 2) {
                filaMusica.getChildren().addAll(centro, controles);
                filaHambre.getChildren().add(hambre);
            } else {
                filaMusica.getChildren().add(centro);
                filaHambre.getChildren().add(controles);
                filaExtra.getChildren().add(hambre);
            }
        }
        filaMusica.setManaged(modo != 0);
        filaMusica.setVisible(modo != 0);
        filaHambre.setManaged(modo >= 2);
        filaHambre.setVisible(modo >= 2);
        filaExtra.setManaged(modo == 3);
        filaExtra.setVisible(modo == 3);
    }

    public void actualizar(int devorados, int sinDevorar) {
        NivelMaw actual = NivelMaw.de(devorados);
        int avance = actual.progreso(devorados);

        izquierda.setText("THE MAW TE OBSERVA · " + devorados
                + (devorados == 1 ? " DEVORADO" : " DEVORADOS") + " · " + sinDevorar + " SIN DEVORAR");
        nivel.setText(actual.name());
        porcentaje.setText(avance + "%");

        int llenos = Math.round(avance * SEGMENTOS / 100f);
        for (int i = 0; i < SEGMENTOS; i++) {
            Node segmento = barra.getChildren().get(i);
            segmento.getStyleClass().remove("pie-segmento-lleno");
            if (i < llenos) segmento.getStyleClass().add("pie-segmento-lleno");
        }
    }

    private static Node crearIconoPlay() {
        Polygon triangulo = new Polygon(0, 0, 12, 7, 0, 14);
        triangulo.getStyleClass().add("icono-musica");
        return triangulo;
    }

    private static Node crearIconoPausa() {
        Rectangle izquierda = new Rectangle(0, 0, 4, 14);
        Rectangle derecha = new Rectangle(8, 0, 4, 14);
        izquierda.getStyleClass().add("icono-musica");
        derecha.getStyleClass().add("icono-musica");
        return new Group(izquierda, derecha);
    }

    private static void fijarTamano(Button boton) {
        boton.setMinSize(ANCHO_BOTON, ALTO_BOTON);
        boton.setPrefSize(ANCHO_BOTON, ALTO_BOTON);
        boton.setMaxSize(ANCHO_BOTON, ALTO_BOTON);
    }
}
