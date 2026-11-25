package consumption.ui;

import consumption.data.Preferencias;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public final class Efectos {

    private static final BooleanProperty CRT_VISIBLE = new SimpleBooleanProperty(true);
    private static Preferencias preferencias;
    private static StackPane raizEscaneo;

    public static void configurar(Preferencias prefs) {
        preferencias = prefs;
        CRT_VISIBLE.set(crtPermitido());
    }

    public static void registrarRaiz(StackPane raiz) {
        raizEscaneo = raiz;
    }

    public static boolean crtPermitido() {
        return preferencias == null || (preferencias.isEfectosActivos() && preferencias.isEfectoCrt());
    }

    public static boolean escaneoPermitido() {
        return preferencias == null || (preferencias.isEfectosActivos() && preferencias.isEfectoEscaneo());
    }

    public static boolean glitchPermitido() {
        return preferencias == null || (preferencias.isEfectosActivos() && preferencias.isEfectoGlitch());
    }

    public static boolean accionesPermitidas() {
        return preferencias == null || (preferencias.isEfectosActivos() && preferencias.isEfectoAcciones());
    }

    public static boolean sonidosPermitidos() {
        return preferencias == null || preferencias.isEfectoSonidos();
    }

    public static boolean arranquePermitido() {
        return preferencias == null || (preferencias.isEfectosActivos() && preferencias.isEfectoArranque());
    }

    public static void actualizar() {
        CRT_VISIBLE.set(crtPermitido());
        StackPane raiz = raizEscaneo;
        if (raiz == null) return;
        if (!escaneoPermitido()) {
            cancelarEscaneo(raiz);
            return;
        }
        boolean corriendo = Boolean.TRUE.equals(raiz.getProperties().get("escaneoActivo"));
        Object espera = raiz.getProperties().get("proximoEscaneo");
        boolean enEspera = espera instanceof Animation a && a.getStatus() == Animation.Status.RUNNING;
        if (!corriendo && !enEspera) programarEscaneo(raiz);
    }

    private static void cancelarEscaneo(StackPane raiz) {
        Object espera = raiz.getProperties().remove("proximoEscaneo");
        if (espera instanceof Animation a) a.stop();
        Object animacion = raiz.getProperties().remove("animacionEscaneo");
        if (animacion instanceof Animation a) a.stop();
        Object soltar = raiz.getProperties().remove("soltarEscaneo");
        if (soltar instanceof Runnable r) r.run();
        Object capa = raiz.getProperties().remove("capaEscaneo");
        if (capa instanceof Node n) raiz.getChildren().remove(n);
        raiz.getProperties().put("escaneoActivo", false);
    }

    private Efectos() {
    }

    public static void glitchAlPasar(Node nodo) {
        PauseTransition fin = new PauseTransition(Duration.millis(150));
        fin.setOnFinished(e -> nodo.setEffect(null));
        nodo.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (!glitchPermitido()) return;
            Sonidos.glitch();
            DropShadow cian = new DropShadow(BlurType.ONE_PASS_BOX,
                    colorTema(nodo, "efecto-glitch-a", "#2fd8ff"), 0, 0, 2, 0);
            DropShadow rojo = new DropShadow(BlurType.ONE_PASS_BOX,
                    colorTema(nodo, "efecto-glitch-b", "#ff2a3d"), 0, 0, -2, 0);
            rojo.setInput(cian);
            nodo.setEffect(rojo);
            fin.playFromStart();
        });
    }

    private static final java.util.Map<String, Color> COLORES_TEMA = new java.util.HashMap<>();

    private static Color colorTema(Node nodo, String clase, String respaldo) {
        try {
            Scene escena = nodo.getScene();
            String clave = clase + "|" + (escena == null ? "" : escena.getStylesheets());
            Color guardado = COLORES_TEMA.get(clave);
            if (guardado != null) return guardado;
            if (escena != null && escena.getRoot() instanceof Pane raiz) {
                Region sonda = new Region();
                sonda.getStyleClass().add(clase);
                sonda.setManaged(false);
                sonda.setMouseTransparent(true);
                raiz.getChildren().add(sonda);
                try {
                    sonda.applyCss();
                    if (sonda.getBackground() != null && !sonda.getBackground().getFills().isEmpty()
                            && sonda.getBackground().getFills().get(0).getFill() instanceof Color color) {
                        COLORES_TEMA.put(clave, color);
                        return color;
                    }
                } finally {
                    raiz.getChildren().remove(sonda);
                }
            }
        } catch (RuntimeException ex) {}
        return Color.web(respaldo);
    }

    public static void escanear(StackPane raiz) {
        if (!escaneoPermitido() || Boolean.TRUE.equals(raiz.getProperties().get("escaneoActivo"))) return;
        raiz.getProperties().put("escaneoActivo", true);
        Sonidos.glitch();
        Pane capa = new Pane();
        capa.setManaged(false);
        capa.setMouseTransparent(true);
        capa.resize(raiz.getWidth(), raiz.getHeight());
        javafx.beans.value.ChangeListener<Number> alAncho =
                (obs, oldV, newV) -> capa.resize(newV.doubleValue(), raiz.getHeight());
        javafx.beans.value.ChangeListener<Number> alAlto =
                (obs, oldV, newV) -> capa.resize(raiz.getWidth(), newV.doubleValue());
        raiz.widthProperty().addListener(alAncho);
        raiz.heightProperty().addListener(alAlto);
        Region estela = new Region();
        estela.getStyleClass().add("efecto-estela");
        estela.setPrefHeight(80);
        estela.setMinHeight(80);
        estela.setMaxHeight(80);
        estela.prefWidthProperty().bind(capa.widthProperty());
        Region nucleo = new Region();
        nucleo.getStyleClass().add("efecto-barrido");
        nucleo.setPrefHeight(11);
        nucleo.setMinHeight(11);
        nucleo.setMaxHeight(11);
        nucleo.prefWidthProperty().bind(capa.widthProperty());
        capa.getChildren().addAll(estela, nucleo);
        capa.setPickOnBounds(false);
        raiz.getChildren().add(capa);
        DoubleProperty avance = new SimpleDoubleProperty(0);
        nucleo.translateYProperty().bind(avance.multiply(raiz.heightProperty().subtract(11)));
        raiz.getProperties().put("capaEscaneo", capa);
        Runnable soltar = () -> {
            raiz.widthProperty().removeListener(alAncho);
            raiz.heightProperty().removeListener(alAlto);
            nucleo.translateYProperty().unbind();
            estela.translateYProperty().unbind();
        };
        raiz.getProperties().put("soltarEscaneo", soltar);
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
        Timeline tramoBajada = new Timeline(
                new KeyFrame(Duration.ZERO, e -> bajada.run(),
                        new KeyValue(avance, 0),
                        new KeyValue(nucleo.opacityProperty(), 0), new KeyValue(estela.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(nucleo.opacityProperty(), .8), new KeyValue(estela.opacityProperty(), .55)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(avance, 1))
        );
        Timeline tramoSubida = new Timeline(
                new KeyFrame(Duration.ZERO, e -> subida.run(),
                        new KeyValue(avance, 1)),
                new KeyFrame(Duration.millis(1850),
                        new KeyValue(avance, 0)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(nucleo.opacityProperty(), 0),
                        new KeyValue(estela.opacityProperty(), 0))
        );
        javafx.animation.SequentialTransition animacion =
                new javafx.animation.SequentialTransition(tramoBajada, tramoSubida);
        raiz.getProperties().put("animacionEscaneo", animacion);
        animacion.setOnFinished(e -> {
            soltar.run();
            raiz.getProperties().remove("soltarEscaneo");
            estela.prefWidthProperty().unbind();
            nucleo.prefWidthProperty().unbind();

            raiz.getChildren().remove(capa);
            raiz.getProperties().put("escaneoActivo", false);
            programarEscaneo(raiz);
        });
        animacion.play();
    }

    private static void programarEscaneo(StackPane raiz) {
        if (!escaneoPermitido() || raiz.getScene() == null || raiz.getScene().getWindow() == null
                || !raiz.getScene().getWindow().isShowing()) return;
        double minutos = 5 + java.util.concurrent.ThreadLocalRandom.current().nextDouble(10);
        PauseTransition espera = new PauseTransition(Duration.minutes(minutos));
        espera.setOnFinished(evento -> {
            if (raiz.getScene() != null && raiz.getScene().getWindow() != null
                    && raiz.getScene().getWindow().isShowing()) escanear(raiz);
        });
        raiz.getProperties().put("proximoEscaneo", espera);
        espera.play();
    }

    public static Parent capa(Parent contenido) {
        Region vineta = new Region();
        vineta.getStyleClass().add("efecto-vineta");
        Region lineas = new Region();
        lineas.getStyleClass().add("efecto-lineas");
        vineta.setMouseTransparent(true);
        lineas.setMouseTransparent(true);
        vineta.visibleProperty().bind(CRT_VISIBLE);
        lineas.visibleProperty().bind(CRT_VISIBLE);

        return new StackPane(contenido, vineta, lineas);
    }
}