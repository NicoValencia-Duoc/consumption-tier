package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class BarraLed extends HBox {

    private static final int SEGMENTOS = 10;

    public BarraLed(Double valor) {
        super(2);
        setAlignment(Pos.CENTER_LEFT);
        int llenos = (valor == null) ? 0 : (int) Math.round(valor);
        for (int i = 0; i < SEGMENTOS; i++) {
            Region segmento = new Region();
            segmento.getStyleClass().add("led-seg");
            if (i < llenos) segmento.getStyleClass().add("led-seg-lleno");
            segmento.setMinSize(8, 12);
            segmento.setPrefSize(8, 12);
            segmento.setMaxSize(8, 12);
            getChildren().add(segmento);
        }
    }
}