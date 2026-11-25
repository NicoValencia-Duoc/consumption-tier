package consumption.ui;

import consumption.model.RachaMaw;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public final class ArranqueTerminal {
    public record Datos(int activos, int pendientes, String nivel, int hambre,
                        int dias, RachaMaw.TierRacha tier) {}

    private static final String CLAVE = "arranqueTerminal";
    private static final int SEGMENTOS = 10;
    private static final String CLAVE_OMITIR = "arranqueTerminalOmitir";
    private static final int INICIO_BARRIDO_MS = 5000;
    private static final int DURACION_BARRIDO_MS = 800;

    private ArranqueTerminal() {}

    private static final class Linea extends TextFlow {
        private final Text[] partes;
        private final String[] textos;
        private final Text cursor = new Text("_");
        private final int largo;

        Linea(String... segmentos) {
            textos = segmentos;
            partes = new Text[segmentos.length];
            int suma = 0;
            for (int i = 0; i < segmentos.length; i++) {
                Text parte = new Text("");
                parte.getStyleClass().add(i % 2 == 0 ? "arranque-texto" : "arranque-valor");
                partes[i] = parte;
                getChildren().add(parte);
                suma += segmentos[i].length();
            }
            largo = suma;
            cursor.getStyleClass().add("arranque-valor");
            cursor.setVisible(false);
            getChildren().add(cursor);
        }

        int largo() {
            return largo;
        }

        void mostrar(int cantidad) {
            int restante = cantidad;
            for (int i = 0; i < partes.length; i++) {
                int usados = Math.max(0, Math.min(textos[i].length(), restante));
                partes[i].setText(textos[i].substring(0, usados));
                restante -= usados;
            }
            cursor.setVisible(cantidad > 0 && cantidad < largo);
        }
    }

    public static StackPane preparar(Parent aplicacion, Datos datos, Runnable alTerminar) {
        StackPane raiz = new StackPane(aplicacion);
        Efectos.registrarRaiz(raiz);

        Text marca = new Text("THE MAW");
        marca.getStyleClass().add("arranque-marca");
        String color = datos.tier().color();
        if (color == null) {
            marca.getStyleClass().add("arranque-marca-base");
        } else {
            marca.setStyle("-fx-fill: " + color + ";");
            marca.setEffect(new DropShadow(26, Color.web(color, 0.7)));
        }
        marca.setOpacity(0);

        Linea lectura = new Linea("> LEYENDO datos.json ........ ", "OK");
        Linea registros = new Linea("> ", plural(datos.activos(), "ENTIDAD ACTIVA", "ENTIDADES ACTIVAS"),
                " · ", plural(datos.pendientes(), "PENDIENTE", "PENDIENTES"));
        Linea nivel = new Linea("> NIVEL ", datos.nivel());
        Linea hambre = new Linea("> HAMBRE");
        hambre.setMinWidth(120);
        hambre.setPrefWidth(120);
        Linea racha = new Linea("> RACHA ", plural(datos.dias(), "DIA", "DIAS"));

        HBox led = new HBox(3);
        led.setAlignment(Pos.CENTER_LEFT);
        int encendidos = Math.round(Math.max(0, Math.min(100, datos.hambre())) * SEGMENTOS / 100f);
        for (int i = 0; i < SEGMENTOS; i++) {
            Region segmento = new Region();
            segmento.getStyleClass().add("arranque-led");
            if (i < encendidos) segmento.getStyleClass().add("arranque-led-on");
            led.getChildren().add(segmento);
        }
        Text porcentaje = new Text(datos.hambre() + "%");
        porcentaje.getStyleClass().add("arranque-valor");
        led.setOpacity(0);
        porcentaje.setOpacity(0);
        HBox filaHambre = new HBox(10, hambre, led, porcentaje);
        filaHambre.setAlignment(Pos.CENTER_LEFT);

        VBox bloque = new VBox(6, lectura, registros, nivel, filaHambre, racha);
        bloque.setAlignment(Pos.CENTER_LEFT);
        bloque.setMinWidth(540);
        bloque.setPrefWidth(540);
        bloque.setMaxWidth(540);
        VBox centro = new VBox(26, marca, bloque);
        centro.setAlignment(Pos.CENTER);

        StackPane cubierta = new StackPane(centro);
        cubierta.getStyleClass().add("arranque-cubierta");
        raiz.getChildren().add(cubierta);

        Region barrido = new Region();
        barrido.getStyleClass().add("arranque-linea");
        barrido.setMinHeight(3);
        barrido.setPrefHeight(3);
        barrido.setMaxHeight(3);
        barrido.setMaxWidth(Double.MAX_VALUE);
        barrido.setMouseTransparent(true);
        barrido.setOpacity(0);
        StackPane.setAlignment(barrido, Pos.TOP_LEFT);
        raiz.getChildren().add(barrido);

        DoubleProperty visible = new SimpleDoubleProperty(1);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(cubierta.widthProperty());
        recorte.heightProperty().bind(cubierta.heightProperty().multiply(visible));
        cubierta.setClip(recorte);
        barrido.translateYProperty().bind(recorte.heightProperty());

        Timeline secuencia = new Timeline();
        secuencia.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(120), e -> { Sonidos.glitch(); marca.setOpacity(0.7); marca.setTranslateX(8); }),
                new KeyFrame(Duration.millis(210), e -> { marca.setOpacity(0.12); marca.setTranslateX(-4); }),
                new KeyFrame(Duration.millis(330), e -> { Sonidos.glitch(); marca.setOpacity(1); marca.setTranslateX(0); }),
                new KeyFrame(Duration.millis(3150), e -> { led.setOpacity(1); porcentaje.setOpacity(1); }),
                new KeyFrame(Duration.millis(INICIO_BARRIDO_MS), e -> barrido.setOpacity(1), new KeyValue(visible, 1)),
                new KeyFrame(Duration.millis(INICIO_BARRIDO_MS + DURACION_BARRIDO_MS),
                        new KeyValue(visible, 0, Interpolator.EASE_IN)));
        escribir(secuencia, lectura, 450, 1250);
        escribir(secuencia, registros, 1350, 2050);
        escribir(secuencia, nivel, 2150, 2650);
        escribir(secuencia, hambre, 2750, 3150);
        escribir(secuencia, racha, 3300, 3800);

        Runnable terminar = () -> {
            cubierta.setClip(null);
            raiz.getChildren().removeAll(cubierta, barrido);
            if (alTerminar != null) alTerminar.run();
            Efectos.escanear(raiz);
        };
        secuencia.setOnFinished(e -> terminar.run());
        raiz.getProperties().put(CLAVE_OMITIR, terminar);
        raiz.getProperties().put(CLAVE, secuencia);
        return raiz;
    }

    public static void iniciar(StackPane raiz) {
        if (Efectos.arranquePermitido()) {
            ((Timeline) raiz.getProperties().get(CLAVE)).play();
            return;
        }
        javafx.application.Platform.runLater((Runnable) raiz.getProperties().get(CLAVE_OMITIR));
    }

    private static void escribir(Timeline secuencia, Linea destino, int desdeMs, int hastaMs) {
        IntegerProperty caracteres = new SimpleIntegerProperty(0);
        caracteres.addListener((obs, anterior, nuevo) -> destino.mostrar(nuevo.intValue()));
        secuencia.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(desdeMs), new KeyValue(caracteres, 0)),
                new KeyFrame(Duration.millis(hastaMs), new KeyValue(caracteres, destino.largo())));
    }

    private static String plural(int cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }
}