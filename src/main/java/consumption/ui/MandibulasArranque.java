package consumption.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.util.Duration;

import java.util.Random;

final class MandibulasArranque extends Pane {
    private static final double ALTO_CONTENIDO = 300;
    private static final double BANDA = 14;
    private static final double MARGEN = 22;
    private static final double PASO = 170;
    private static final double LARGO_MAXIMO = 1.45;
    private static final double FACTOR_CAIDA = 0.34;

    private final Group superior = new Group();
    private final Group inferior = new Group();
    private final DoubleProperty caida = new SimpleDoubleProperty(0);
    private double caidaMaxima;

    MandibulasArranque() {
        setMouseTransparent(true);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().addAll(superior, inferior);
        widthProperty().addListener((obs, antes, ahora) -> rehacer());
        heightProperty().addListener((obs, antes, ahora) -> rehacer());
        caida.addListener((obs, antes, ahora) -> mover(ahora.doubleValue()));
    }

    void programar(Timeline linea) {
        amago(linea, 700);
        titilar(linea, 1500);
        amago(linea, 2200);
        titilar(linea, 2900);
        amago(linea, 3600);
    }

    private void amago(Timeline linea, int inicio) {
        linea.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(inicio), new KeyValue(caida, 0)),
                new KeyFrame(Duration.millis(inicio + 140), new KeyValue(caida, 1, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(inicio + 260), new KeyValue(caida, 0.7)),
                new KeyFrame(Duration.millis(inicio + 330), new KeyValue(caida, 1)),
                new KeyFrame(Duration.millis(inicio + 800), new KeyValue(caida, 0, Interpolator.EASE_OUT)));
    }

    private void titilar(Timeline linea, int inicio) {
        double[][] pasos = {{0, 1, 1}, {80, 0.25, 1}, {160, 1, 0.2}, {240, 0.3, 1}, {330, 1, 0.25}, {420, 1, 1}};
        for (double[] paso : pasos) {
            Duration momento = Duration.millis(inicio + paso[0]);
            linea.getKeyFrames().add(new KeyFrame(momento,
                    new KeyValue(superior.opacityProperty(), paso[1], Interpolator.DISCRETE),
                    new KeyValue(inferior.opacityProperty(), paso[2], Interpolator.DISCRETE)));
        }
    }

    private void mover(double valor) {
        superior.setTranslateY(valor * caidaMaxima);
        inferior.setTranslateY(-valor * caidaMaxima);
    }

    private void rehacer() {
        double ancho = getWidth();
        double alto = getHeight();
        if (ancho < 1 || alto < 1) return;
        double espacio = (alto - ALTO_CONTENIDO) / 2;
        double diente = (espacio - BANDA - MARGEN) / (LARGO_MAXIMO + FACTOR_CAIDA);
        diente = Math.max(18, Math.min(90, diente));
        caidaMaxima = diente * FACTOR_CAIDA;
        int cantidad = Math.max(4, (int) Math.round(ancho / PASO));
        double paso = ancho / cantidad;
        construir(superior, true, ancho, alto, cantidad, paso, diente, new Random(7));
        construir(inferior, false, ancho, alto, cantidad, paso, diente, new Random(19));
        mover(caida.get());
    }

    private void construir(Group grupo, boolean arriba, double ancho, double alto, int cantidad,
                           double paso, double diente, Random azar) {
        double signo = arriba ? 1 : -1;
        double origen = arriba ? 0 : alto;
        double desfase = arriba ? 0 : -paso / 2;
        double nace = origen + signo * BANDA;

        Path forma = new Path();
        forma.getStyleClass().add("arranque-mandibula");
        forma.getElements().add(new MoveTo(0, origen - signo * 8));
        forma.getElements().add(new LineTo(ancho, origen - signo * 8));
        forma.getElements().add(new LineTo(ancho, nace));
        for (int i = cantidad; i >= 0; i--) {
            double x = desfase + i * paso;
            double largo = diente * (0.55 + 0.9 * azar.nextDouble());
            double punta = nace + signo * largo;
            forma.getElements().add(new LineTo(x + paso * 0.5, nace));
            forma.getElements().add(new QuadCurveTo(x + paso * 0.32, nace + signo * largo * 0.6,
                    x + paso * 0.1, punta));
            forma.getElements().add(new QuadCurveTo(x - paso * 0.08, nace + signo * largo * 0.35,
                    x - paso * 0.18, nace));
        }
        forma.getElements().add(new LineTo(0, nace));
        forma.getElements().add(new ClosePath());

        grupo.getChildren().setAll(forma);
        for (int i = 0; i < cantidad; i++) {
            Circle punto = new Circle(i * paso + paso * 0.5, nace - signo * 9, 2.2);
            punto.getStyleClass().add("arranque-mandibula-punto");
            grupo.getChildren().add(punto);
        }
    }
}
