package consumption.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class Aviso extends StackPane {

    private final Label texto = new Label();
    private SequentialTransition animacion;

    public Aviso() {
        setMouseTransparent(true);
        texto.getStyleClass().add("aviso-maw");
        texto.setVisible(false);
        StackPane.setAlignment(texto, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(texto, new Insets(0, 8, 8, 0));
        getChildren().add(texto);
    }

    public void mostrar(String mensaje) {
        if (animacion != null) animacion.stop();
        texto.setText(mensaje);
        texto.setOpacity(0);
        texto.setVisible(true);

        FadeTransition entra = new FadeTransition(Duration.millis(180), texto);
        entra.setToValue(1);
        PauseTransition espera = new PauseTransition(Duration.seconds(2.2));
        FadeTransition sale = new FadeTransition(Duration.millis(450), texto);
        sale.setToValue(0);

        animacion = new SequentialTransition(entra, espera, sale);
        animacion.setOnFinished(e -> texto.setVisible(false));
        animacion.play();
    }
}