package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class PieMaw extends HBox {

    private static final int SEGMENTOS = 10;

    private final Label izquierda = new Label();
    private final Label nivel = new Label();
    private final Label porcentaje = new Label();
    private final HBox barra = new HBox(2);

    public PieMaw() {
        getStyleClass().add("pie-maw");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

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

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        getChildren().addAll(punto, izquierda, espacio, etiquetaNivel, nivel, etiquetaHambre, barra, porcentaje);
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
}