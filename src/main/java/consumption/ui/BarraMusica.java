package consumption.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;

public class BarraMusica extends VBox {

    private static final double ANCHO_BOTON = 52;
    private static final double ALTO_BOTON = 28;

    public BarraMusica(ReproductorMusica reproductor) {
        getStyleClass().add("reproductor");

        LineaProgreso linea = new LineaProgreso(reproductor.progresoProperty(), reproductor::buscar);

        Button pausa = new Button();
        pausa.getStyleClass().add("boton-musica");
        Node iconoPlay = crearIconoPlay();
        Node iconoPausa = crearIconoPausa();
        pausa.setGraphic(reproductor.sonandoProperty().get() ? iconoPausa : iconoPlay);
        reproductor.sonandoProperty().addListener((obs, antes, ahora) -> pausa.setGraphic(ahora ? iconoPausa : iconoPlay));
        pausa.setOnAction(e -> reproductor.alternarPausa());
        fijarAncho(pausa);

        Button siguiente = new Button("▶▶");
        siguiente.getStyleClass().add("boton-musica");
        siguiente.setOnAction(e -> reproductor.siguiente());
        fijarAncho(siguiente);

        Button lista = new Button("☰");
        lista.getStyleClass().add("boton-musica");
        lista.setOnAction(e -> PistasDialog.mostrar(reproductor, getScene().getStylesheets()));
        fijarAncho(lista);

        Slider volumen = new Slider(0, 1, reproductor.volumenProperty().get());
        volumen.setPrefWidth(110);
        reproductor.volumenProperty().bind(volumen.valueProperty());

        Label titulo = new Label();
        titulo.getStyleClass().add("cancion");
        titulo.setMaxWidth(420);
        titulo.textProperty().bind(Bindings.concat("♪ ", reproductor.tituloProperty()));

        Label tiempo = new Label();
        tiempo.getStyleClass().add("tiempo");
        tiempo.setMinWidth(112);
        tiempo.textProperty().bind(reproductor.tiempoProperty());

        HBox fila = new HBox(10, titulo, tiempo, lista, pausa, siguiente, volumen);
        fila.setAlignment(Pos.CENTER_RIGHT);
        fila.getStyleClass().add("reproductor-fila");

        getChildren().addAll(linea, fila);

        if (!reproductor.hayMusica()) {
            linea.setDisable(true);
            pausa.setDisable(true);
            siguiente.setDisable(true);
            volumen.setDisable(true);
            lista.setDisable(true);
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

    private static void fijarAncho(Button boton) {
        boton.setMinSize(ANCHO_BOTON, ALTO_BOTON);
        boton.setPrefSize(ANCHO_BOTON, ALTO_BOTON);
        boton.setMaxSize(ANCHO_BOTON, ALTO_BOTON);
    }
}