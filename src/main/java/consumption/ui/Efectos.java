package consumption.ui;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class Efectos {

    public static final boolean ACTIVOS = true;

    private Efectos() {
    }

    public static void glitchAlPasar(Node nodo) {
        if (!ACTIVOS) return;
        DropShadow cian = new DropShadow(BlurType.ONE_PASS_BOX, Color.web("#2fd8ff"), 0, 0, 2, 0);
        DropShadow rojo = new DropShadow(BlurType.ONE_PASS_BOX, Color.web("#ff2a3d"), 0, 0, -2, 0);
        rojo.setInput(cian);
        PauseTransition fin = new PauseTransition(Duration.millis(150));
        fin.setOnFinished(e -> nodo.setEffect(null));
        nodo.setOnMouseEntered(e -> {
            nodo.setEffect(rojo);
            fin.playFromStart();
        });
    }

    public static Parent capa(Parent contenido) {
        if (!ACTIVOS) return contenido;

        Region vineta = new Region();
        vineta.getStyleClass().add("efecto-vineta");
        Region lineas = new Region();
        lineas.getStyleClass().add("efecto-lineas");
        vineta.setMouseTransparent(true);
        lineas.setMouseTransparent(true);

        return new StackPane(contenido, vineta, lineas);
    }
}