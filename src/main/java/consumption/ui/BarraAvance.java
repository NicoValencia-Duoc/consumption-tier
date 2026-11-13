package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class BarraAvance extends HBox {

    private final ProgressBar barra = new ProgressBar(0);
    private final Label texto = new Label("0%");

    public BarraAvance() {
        super(8);
        getStyleClass().add("barra-avance");
        setAlignment(Pos.CENTER_LEFT);
        barra.setPrefWidth(170);
        barra.setPrefHeight(12);
        texto.getStyleClass().add("valor-criterio");
        getChildren().addAll(barra, texto);
    }

    public BarraAvance(double porcentaje) {
        this();
        setPorcentaje(porcentaje);
    }

    public void setPorcentaje(double porcentaje) {
        barra.setProgress(Math.max(0, Math.min(porcentaje, 100)) / 100.0);
        texto.setText(String.format("%.0f%%", porcentaje));
        getStyleClass().remove("exceso");
        if (porcentaje > 100) getStyleClass().add("exceso");
    }
}