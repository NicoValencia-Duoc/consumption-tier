package consumption.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

public class BarraMusica extends HBox {

    public BarraMusica(ReproductorMusica reproductor) {
        setSpacing(10);
        setAlignment(Pos.CENTER_RIGHT);
        getStyleClass().add("reproductor");

        Button pausa = new Button();
        pausa.getStyleClass().add("boton-musica");
        pausa.textProperty().bind(Bindings.when(reproductor.sonandoProperty()).then("❚❚").otherwise("▶"));
        pausa.setOnAction(e -> reproductor.alternarPausa());

        Button siguiente = new Button("▶▶");
        siguiente.getStyleClass().add("boton-musica");
        siguiente.setOnAction(e -> reproductor.siguiente());

        Button lista = new Button("☰");
        lista.getStyleClass().add("boton-musica");
        lista.setOnAction(e -> PistasDialog.mostrar(reproductor, getScene().getStylesheets()));

        Slider volumen = new Slider(0, 1, reproductor.volumenProperty().get());
        volumen.setPrefWidth(110);
        reproductor.volumenProperty().bind(volumen.valueProperty());

        Label titulo = new Label();
        titulo.getStyleClass().add("cancion");
        titulo.setMaxWidth(420);
        titulo.textProperty().bind(Bindings.concat("♪ ", reproductor.tituloProperty()));

        getChildren().addAll(titulo, lista, pausa, siguiente, volumen);

        if (!reproductor.hayMusica()) {
            pausa.setDisable(true);
            siguiente.setDisable(true);
            volumen.setDisable(true);
            lista.setDisable(true);
        }
    }
}