package consumption.ui;

import javafx.beans.value.ObservableValue;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.function.DoublePredicate;

public class LineaProgreso extends Pane {

    private static final double ALTO = 10;
    private static final double GROSOR = 3;
    private static final double GROSOR_ACTIVO = 6;

    private final Region pista = new Region();
    private final Region relleno = new Region();
    private double fraccion = 0;
    private boolean arrastrando = false;

    public LineaProgreso(ObservableValue<Number> progreso, DoublePredicate alBuscar) {
        getStyleClass().add("linea-progreso");
        pista.getStyleClass().add("linea-progreso-pista");
        relleno.getStyleClass().add("linea-progreso-relleno");
        for (Region parte : new Region[]{pista, relleno}) {
            parte.setManaged(false);
            parte.setMouseTransparent(true);
        }
        getChildren().addAll(pista, relleno);

        setMinHeight(ALTO);
        setPrefHeight(ALTO);
        setMaxHeight(ALTO);

        progreso.addListener((obs, antes, ahora) -> {
            if (!arrastrando) poner(ahora.doubleValue());
        });
        hoverProperty().addListener((obs, antes, ahora) -> requestLayout());

        setOnMousePressed(e -> {
            arrastrando = true;
            poner(fraccionDe(e));
        });
        setOnMouseDragged(e -> poner(fraccionDe(e)));
        setOnMouseReleased(e -> {
            arrastrando = false;
            if (alBuscar.test(fraccionDe(e))) {
                requestLayout();
            } else {
                poner(progreso.getValue().doubleValue());
            }
        });
    }

    @Override
    protected void layoutChildren() {
        double grosor = (isHover() || arrastrando) ? GROSOR_ACTIVO : GROSOR;
        double ancho = getWidth();
        pista.resizeRelocate(0, 0, ancho, grosor);
        relleno.resizeRelocate(0, 0, ancho * fraccion, grosor);
    }

    private void poner(double valor) {
        fraccion = Math.max(0, Math.min(1, valor));
        requestLayout();
    }

    private double fraccionDe(MouseEvent e) {
        double ancho = getWidth();
        return ancho <= 0 ? 0 : e.getX() / ancho;
    }
}