package consumption.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
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
        cancion.setMaxWidth(220);
        cancion.textProperty().bind(Bindings.concat("♪ ", reproductor.tituloProperty()));

        Label tiempo = new Label();
        tiempo.getStyleClass().add("tiempo");
        tiempo.setMinWidth(100);
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
        volumen.setPrefWidth(90);
        reproductor.volumenProperty().bind(volumen.valueProperty());

        Region separador = new Region();
        separador.getStyleClass().add("separador-pestanas");
        separador.setMinSize(1, 16);
        separador.setPrefSize(1, 16);
        separador.setMaxSize(1, 16);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox fila = new HBox(10, punto, izquierda, espacio, cancion, tiempo, lista, pausa, siguiente, volumen,
                separador, etiquetaNivel, nivel, etiquetaHambre, barra, porcentaje);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("pie-fila");

        getChildren().addAll(linea, fila);

        if (!reproductor.hayMusica()) {
            linea.setDisable(true);
            lista.setDisable(true);
            pausa.setDisable(true);
            siguiente.setDisable(true);
            volumen.setDisable(true);
        }
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
