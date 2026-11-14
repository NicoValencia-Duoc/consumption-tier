package consumption.ui;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class TooltipoComentario {

    private static final Duration ESPERA = Duration.seconds(2);

    private TooltipoComentario() {
    }

    public static Tooltip de(String comentario) {
        if (comentario == null || comentario.isBlank()) return null;
        Tooltip globo = new Tooltip(comentario);
        globo.setWrapText(true);
        globo.setMaxWidth(380);
        globo.setShowDelay(ESPERA);
        globo.setShowDuration(Duration.INDEFINITE);
        globo.setHideDelay(Duration.millis(100));
        return globo;
    }
}
