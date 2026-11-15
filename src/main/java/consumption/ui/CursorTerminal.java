package consumption.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class CursorTerminal extends Label {

    private final Timeline parpadeo = new Timeline(
            new KeyFrame(Duration.ZERO, e -> setOpacity(1)),
            new KeyFrame(Duration.millis(550), e -> setOpacity(0)),
            new KeyFrame(Duration.millis(1100)));

    public CursorTerminal() {
        super("▌");
        getStyleClass().add("titulo-terminal");
        parpadeo.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((obs, antes, escena) -> {
            if (escena == null) parpadeo.stop();
            else parpadeo.play();
        });
    }
}