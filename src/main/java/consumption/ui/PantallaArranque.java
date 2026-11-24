package consumption.ui;

import javafx.animation.*;
import javafx.scene.Parent;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import java.util.Random;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class PantallaArranque {
    private PantallaArranque() {}

    public static StackPane preparar(Parent aplicacion) {
        StackPane raiz = new StackPane(aplicacion);
        StackPane cubierta = new StackPane();
        cubierta.setStyle("-fx-background-color: #09090c;");
        cubierta.setMouseTransparent(false);

        Label marca = new Label("THE MAW");
        marca.setStyle("-fx-font-family: 'Orbitron'; -fx-font-size: 76px; -fx-font-weight: bold; -fx-text-fill: #fcee0a;");
        marca.setEffect(new DropShadow(28, Color.web("#fcee0a", .65)));
        Label mensaje = new Label("THE MAW DESPIERTA...");
        mensaje.setStyle("-fx-font-family: 'Share Tech Mono'; -fx-font-size: 20px; -fx-text-fill: #b9b9b9;");
        VBox centro = new VBox(18, marca, mensaje);
        centro.setAlignment(javafx.geometry.Pos.CENTER);
        centro.setMaxWidth(Double.MAX_VALUE);
        marca.setMaxWidth(Double.MAX_VALUE);
        mensaje.setMaxWidth(Double.MAX_VALUE);
        marca.setAlignment(javafx.geometry.Pos.CENTER);
        mensaje.setAlignment(javafx.geometry.Pos.CENTER);
        StackPane.setAlignment(centro, javafx.geometry.Pos.CENTER);
        cubierta.getChildren().add(centro);
        raiz.getChildren().add(cubierta);

        marca.setOpacity(0);
        mensaje.setOpacity(0);
        Pane mordida = new Pane();
        mordida.setMouseTransparent(true);
        mordida.prefWidthProperty().bind(cubierta.widthProperty());
        mordida.prefHeightProperty().bind(cubierta.heightProperty());
        cubierta.getChildren().add(mordida);
        Polyline superior = new Polyline();
        Polyline inferior = new Polyline();
        for (Polyline borde : new Polyline[]{superior, inferior}) {
            borde.setFill(null);
            borde.setStrokeWidth(3.3);
            borde.setStrokeLineJoin(StrokeLineJoin.ROUND);
            borde.getStyleClass().add("mandibula-neon");
            borde.setEffect(new DropShadow(19, Color.web("#c43cff", .75)));
            mordida.getChildren().add(borde);
        }
        Runnable actualizarDientes = () -> {
            double w = mordida.getWidth(), h = mordida.getHeight();
            if (w < 1 || h < 1) return;
            superior.getPoints().clear(); inferior.getPoints().clear();
            double[] xs = {0,.09,.18,.24,.31,.39,.46,.53,.60,.68,.76,.83,.91,1};
            double[] alturas = {-.12,.12,.27,.04,.32,.11,.37,.06,.33,.09,.29,.04,.18,-.12};
            for (int i=0;i<xs.length;i++) {
                superior.getPoints().addAll(xs[i]*w, alturas[i]*h*.55);
                inferior.getPoints().addAll(xs[i]*w, h-alturas[i]*h*.55);
            }
        };
        mordida.widthProperty().addListener((o,ant,nuevo)->actualizarDientes.run());
        mordida.heightProperty().addListener((o,ant,nuevo)->actualizarDientes.run());
        superior.setTranslateY(-1100); inferior.setTranslateY(1100);
        superior.setOpacity(0); inferior.setOpacity(0);
        Pane fragmentos = new Pane();
        fragmentos.setMouseTransparent(true);
        fragmentos.setManaged(false);
        fragmentos.resize(cubierta.getWidth(), cubierta.getHeight());
        cubierta.widthProperty().addListener((obs, oldV, newV) -> fragmentos.resize(newV.doubleValue(), cubierta.getHeight()));
        cubierta.heightProperty().addListener((obs, oldV, newV) -> fragmentos.resize(cubierta.getWidth(), newV.doubleValue()));
        cubierta.getChildren().add(fragmentos);
        Random azar = new Random(30);
        Rectangle[] esquirlas = new Rectangle[90];
        for (int i = 0; i < esquirlas.length; i++) {
            Rectangle r = new Rectangle(7 + azar.nextInt(17), 3 + azar.nextInt(7));
            r.getStyleClass().add("fragmento-mordida");
            r.setOpacity(0);
            esquirlas[i] = r;
            fragmentos.getChildren().add(r);
        }
        Runnable prepararFragmentos = () -> {
            double w = fragmentos.getWidth(), h = fragmentos.getHeight();
            Random r = new Random(30);
            for (Rectangle trozo : esquirlas) {
                trozo.setX(w / 2 + (r.nextDouble() - .5) * 410);
                trozo.setY(h / 2 + (r.nextDouble() - .5) * 95);
                trozo.setTranslateX(0);
                trozo.setTranslateY(0);
                trozo.setOpacity(0);
            }
        };
        Timeline secuencia = new Timeline(
                new KeyFrame(Duration.ZERO, e -> { marca.setOpacity(0); mensaje.setOpacity(0); marca.setTranslateX(-9); actualizarDientes.run(); }),
                new KeyFrame(Duration.millis(240), e -> { marca.setOpacity(.72); marca.setTranslateX(8); }),
                new KeyFrame(Duration.millis(340), e -> { marca.setOpacity(.12); marca.setTranslateX(-4); }),
                new KeyFrame(Duration.millis(460), e -> { marca.setOpacity(1); marca.setTranslateX(0); }),
                new KeyFrame(Duration.millis(650), new KeyValue(superior.opacityProperty(), 1), new KeyValue(inferior.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(780), new KeyValue(mensaje.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(1600), new KeyValue(superior.translateYProperty(), 430), new KeyValue(inferior.translateYProperty(), -430)),
                new KeyFrame(Duration.millis(1690), e -> { marca.setTranslateX(10); }),
                new KeyFrame(Duration.millis(1770), e -> { marca.setTranslateX(-7); }),
                new KeyFrame(Duration.millis(1850), e -> { marca.setTranslateX(0); }),
                new KeyFrame(Duration.millis(1890), e -> {
                    prepararFragmentos.run();
                    marca.setOpacity(0); mensaje.setOpacity(0);
                    for (Rectangle r : esquirlas) r.setOpacity(.95);
                }),
                new KeyFrame(Duration.millis(1900), new KeyValue(cubierta.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(2010), e -> {
                    for (int i = 0; i < esquirlas.length; i++) {
                        Rectangle r = esquirlas[i];
                        r.setTranslateX((i % 2 == 0 ? 1 : -1) * (25 + i % 43));
                        r.setTranslateY((i % 3 - 1) * (16 + i % 31));
                        r.setOpacity(.85);
                    }
                }),
                new KeyFrame(Duration.millis(2310), e -> { for (Rectangle r : esquirlas) r.setOpacity(.35); },
                        new KeyValue(superior.opacityProperty(), .6), new KeyValue(inferior.opacityProperty(), .6)),
                new KeyFrame(Duration.millis(2450), e -> { for (Rectangle r : esquirlas) r.setOpacity(0); }, new KeyValue(cubierta.opacityProperty(), .65)),
                new KeyFrame(Duration.millis(2800), new KeyValue(cubierta.opacityProperty(), 0))
        );
        secuencia.setOnFinished(e -> { raiz.getChildren().remove(cubierta); Efectos.escanear(raiz); });
        raiz.getProperties().put("pantallaArranqueTimeline", secuencia);
        return raiz;
    }

    public static void iniciar(StackPane raiz) {
        ((Timeline) raiz.getProperties().get("pantallaArranqueTimeline")).play();
    }
}