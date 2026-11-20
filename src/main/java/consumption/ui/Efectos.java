package consumption.ui;

import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
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

    public static void escanear(StackPane raiz) {
        if (!ACTIVOS || Boolean.TRUE.equals(raiz.getProperties().get("c30ScanActive"))) return;
        raiz.getProperties().put("c30ScanActive", true);
        Pane capa = new Pane();
        capa.setManaged(false);
        capa.setMouseTransparent(true);
        capa.resize(raiz.getWidth(), raiz.getHeight());
        raiz.widthProperty().addListener((obs, oldV, newV) -> capa.resize(newV.doubleValue(), raiz.getHeight()));
        raiz.heightProperty().addListener((obs, oldV, newV) -> capa.resize(raiz.getWidth(), newV.doubleValue()));
        Region estela = new Region();
        estela.getStyleClass().add("efecto-estela-c30");
        estela.setPrefHeight(80);
        estela.setMinHeight(80);
        estela.setMaxHeight(80);
        estela.prefWidthProperty().bind(capa.widthProperty());
        Region nucleo = new Region();
        nucleo.getStyleClass().add("efecto-barrido-c30");
        nucleo.setPrefHeight(11);
        nucleo.setMinHeight(11);
        nucleo.setMaxHeight(11);
        nucleo.prefWidthProperty().bind(capa.widthProperty());
        capa.getChildren().addAll(estela, nucleo);
        capa.setPickOnBounds(false);
        raiz.getChildren().add(capa);
        double altura = Math.max(1, raiz.getHeight() - 11);
        javafx.beans.property.BooleanProperty descendiendo = new javafx.beans.property.SimpleBooleanProperty(true);
        estela.translateYProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> nucleo.getTranslateY() + (descendiendo.get() ? -80 : 11),
                nucleo.translateYProperty(), descendiendo));
        Runnable bajada = () -> {
            descendiendo.set(true);
            estela.setStyle("-fx-background-color: linear-gradient(to bottom, transparent, -c-acento);");
        };
        Runnable subida = () -> {
            descendiendo.set(false);
            estela.setStyle("-fx-background-color: linear-gradient(to bottom, -c-acento, transparent);");
        };
        nucleo.setOpacity(0); estela.setOpacity(0);
        Timeline tramoBajada1 = new Timeline(
                new KeyFrame(Duration.ZERO, e -> bajada.run(),
                        new KeyValue(nucleo.translateYProperty(), 0),
                        new KeyValue(nucleo.opacityProperty(), 0), new KeyValue(estela.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(nucleo.opacityProperty(), .8), new KeyValue(estela.opacityProperty(), .55)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(nucleo.translateYProperty(), altura))
        );
        Timeline tramoSubida = new Timeline(
                new KeyFrame(Duration.ZERO, e -> subida.run(),
                        new KeyValue(nucleo.translateYProperty(), altura)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(nucleo.translateYProperty(), 0))
        );
        Timeline tramoBajada2 = new Timeline(
                new KeyFrame(Duration.ZERO, e -> bajada.run(),
                        new KeyValue(nucleo.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(1850),
                        new KeyValue(nucleo.translateYProperty(), altura)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(nucleo.opacityProperty(), 0),
                        new KeyValue(estela.opacityProperty(), 0))
        );
        javafx.animation.SequentialTransition animacion =
                new javafx.animation.SequentialTransition(tramoBajada1, tramoSubida, tramoBajada2);
        animacion.setOnFinished(e -> {
            estela.translateYProperty().unbind();
            estela.prefWidthProperty().unbind();
            nucleo.prefWidthProperty().unbind();

            raiz.getChildren().remove(capa);
            raiz.getProperties().put("c30ScanActive", false);
            if (raiz.getScene() != null && raiz.getScene().getWindow() != null && raiz.getScene().getWindow().isShowing()) {
                double minutos = 5 + java.util.concurrent.ThreadLocalRandom.current().nextDouble(10);
                String prueba = System.getProperty("c30.scan.testSeconds");
                Duration demora = (prueba != null && prueba.equals("30"))
                        ? Duration.seconds(30) : Duration.minutes(minutos);
                System.out.println("[C30-SCAN] Próximo escaneo en " + demora.toSeconds() + " segundos");
                PauseTransition espera = new PauseTransition(demora);
                espera.setOnFinished(evento -> {
                    if (raiz.getScene() != null && raiz.getScene().getWindow() != null && raiz.getScene().getWindow().isShowing()) escanear(raiz);
                });
                raiz.getProperties().put("c30NextScan", espera);
                espera.play();
            }
        });
        animacion.play();
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