package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class BarritaAvance extends HBox {

    private static final double ANCHO = 90;
    private static final double ALTO = 8;

    public BarritaAvance(double porcentaje) {
        super(8);
        setAlignment(Pos.CENTER_LEFT);

        double normal = ReglasTabla.fraccionNormal(porcentaje) * ANCHO;
        double exceso = ReglasTabla.fraccionExceso(porcentaje) * ANCHO;
        boolean pasaDeCien = porcentaje > 100;

        Pane pista = new Pane();
        pista.getStyleClass().add("barrita-pista");
        pista.setMinSize(ANCHO, ALTO);
        pista.setPrefSize(ANCHO, ALTO);
        pista.setMaxSize(ANCHO, ALTO);

        Region relleno = new Region();
        relleno.getStyleClass().add("barrita-normal");
        relleno.setPrefSize(normal, ALTO);
        pista.getChildren().add(relleno);

        if (pasaDeCien) {
            Region sobra = new Region();
            sobra.getStyleClass().add("barrita-exceso");
            sobra.setLayoutX(normal);
            sobra.setPrefSize(exceso, ALTO);

            Region marca = new Region();
            marca.getStyleClass().add("barrita-marca");
            marca.setLayoutX(ANCHO / 2);
            marca.setLayoutY(-2);
            marca.setPrefSize(1, ALTO + 4);

            pista.getChildren().addAll(sobra, marca);
        }

        Label texto = new Label(String.format("%.0f%%", porcentaje));
        texto.getStyleClass().add("barrita-texto");
        if (pasaDeCien) texto.getStyleClass().add("exceso");

        getChildren().addAll(pista, texto);
    }
}