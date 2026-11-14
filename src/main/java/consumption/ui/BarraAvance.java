package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;

public class BarraAvance extends HBox {

    private static final double ANCHO = 200;
    private static final double ALTO = 12;

    private final Region pista = new Region();
    private final Region normal = new Region();
    private final Region exceso = new Region();
    private final Region marca = new Region();
    private final StackPane caja = new StackPane(pista, normal, exceso, marca);
    private final Label texto = new Label("0%");

    private double ultimo;
    private boolean extendida;

    public BarraAvance() {
        super(8);
        getStyleClass().add("barra-avance");
        setAlignment(Pos.CENTER_LEFT);

        pista.getStyleClass().add("avance-pista");
        normal.getStyleClass().add("avance-normal");
        exceso.getStyleClass().add("avance-exceso");
        marca.getStyleClass().add("avance-marca");

        for (Region r : List.of(pista, normal, exceso, marca)) {
            r.setMinHeight(ALTO);
            r.setPrefHeight(ALTO);
            r.setMaxHeight(ALTO);
            StackPane.setAlignment(r, Pos.CENTER_LEFT);
        }
        fijarAncho(pista, ANCHO);
        fijarAncho(marca, 2);

        caja.setMinSize(ANCHO, ALTO);
        caja.setPrefSize(ANCHO, ALTO);
        caja.setMaxSize(ANCHO, ALTO);

        texto.getStyleClass().add("valor-criterio");
        getChildren().addAll(caja, texto);
        redibujar();
    }

    public BarraAvance(double porcentaje) {
        this();
        setPorcentaje(porcentaje);
    }

    public void setPorcentaje(double porcentaje) {
        this.ultimo = porcentaje;
        redibujar();
    }

    public void setExtendida(boolean extendida) {
        this.extendida = extendida;
        redibujar();
    }

    static double[] proporciones(double porcentaje, boolean extendida) {
        double p = Math.max(0, porcentaje);
        boolean ampliada = extendida || p > 100;
        double escala = ampliada ? 200 : 100;
        double base = Math.min(p, 100) / escala;
        double sobra = ampliada ? (Math.min(p, 200) - Math.min(p, 100)) / escala : 0;
        double posicionMarca = ampliada ? 0.5 : -1;
        return new double[]{base, sobra, posicionMarca};
    }

    private void redibujar() {
        double[] v = proporciones(ultimo, extendida);
        fijarAncho(normal, ANCHO * v[0]);
        fijarAncho(exceso, ANCHO * v[1]);
        exceso.setTranslateX(ANCHO * v[0]);
        exceso.setVisible(v[1] > 0);
        marca.setVisible(v[2] >= 0);
        marca.setTranslateX(ANCHO * 0.5 - 1);

        texto.setText(String.format("%.0f%%", ultimo));
        getStyleClass().remove("exceso");
        if (ultimo > 100) getStyleClass().add("exceso");
    }

    private static void fijarAncho(Region r, double ancho) {
        r.setMinWidth(ancho);
        r.setPrefWidth(ancho);
        r.setMaxWidth(ancho);
    }
}