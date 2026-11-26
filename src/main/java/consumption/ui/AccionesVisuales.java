package consumption.ui;

import consumption.model.ItemConsumption;
import consumption.model.NivelMaw;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AccionesVisuales {
    private static final int INTENTOS = 8;
    private static final double ALTO_BANDA = 84;
    private static final double ALTO_BANDA_TARJETA = 70;

    private final Pane capa;
    private final Node contenido;

    public AccionesVisuales(Pane capa, Node contenido) {
        this.capa = capa;
        this.contenido = contenido;
    }

    public void destacar(ItemConsumption item, boolean devorado) {
        if (devorado) Sonidos.devorar(); else Sonidos.guardar();
        if (!Efectos.accionesPermitidas()) return;
        esperarNodo(item, 0, zona -> {
            if (devorado) barrido(zona); else parpadeo(zona);
        });
    }

    public List<Bounds> capturar(List<? extends ItemConsumption> lote) {
        List<Bounds> zonas = new ArrayList<>();
        if (!Efectos.accionesPermitidas()) return zonas;
        for (ItemConsumption item : lote) {
            Bounds zona = zonaDe(buscarNodo(item));
            if (zona != null) zonas.add(zona);
        }
        return zonas;
    }

    public void corromper(List<Bounds> zonas) {
        Sonidos.eliminar();
        if (!Efectos.accionesPermitidas()) return;
        for (Bounds zona : zonas) {
            Rectangle fantasma = new Rectangle(zona.getMinX(), zona.getMinY(), zona.getWidth(), zona.getHeight());
            fantasma.getStyleClass().add("efecto-corrupcion");
            fantasma.setOpacity(0);
            capa.getChildren().add(fantasma);
            Timeline linea = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(fantasma.opacityProperty(), 0.5)),
                    new KeyFrame(Duration.millis(60), new KeyValue(fantasma.translateXProperty(), -4)),
                    new KeyFrame(Duration.millis(120), new KeyValue(fantasma.translateXProperty(), 4)),
                    new KeyFrame(Duration.millis(180), new KeyValue(fantasma.translateXProperty(), 0),
                            new KeyValue(fantasma.scaleYProperty(), 1)),
                    new KeyFrame(Duration.millis(350), new KeyValue(fantasma.scaleYProperty(), 0.04)),
                    new KeyFrame(Duration.millis(520), new KeyValue(fantasma.opacityProperty(), 0)));
            linea.setOnFinished(e -> capa.getChildren().remove(fantasma));
            linea.play();
        }
    }

    public void subirNivel(NivelMaw antes, NivelMaw despues) {
        Sonidos.nivel();
        if (!Efectos.accionesPermitidas()) return;
        Text titulo = new Text(antes.etiqueta());
        titulo.getStyleClass().add("efecto-banda-titulo");
        Text subtitulo = new Text("> HAMBRE SACIADA // EL NIVEL SUBE");
        subtitulo.getStyleClass().add("efecto-banda-sub");
        VBox banda = new VBox(4, titulo, subtitulo);
        banda.getStyleClass().add("efecto-banda");
        banda.setAlignment(Pos.CENTER);
        banda.setOpacity(0);
        banda.prefWidthProperty().bind(capa.widthProperty());
        banda.setMinHeight(ALTO_BANDA);
        banda.setPrefHeight(ALTO_BANDA);
        banda.setMaxHeight(ALTO_BANDA);
        banda.layoutYProperty().bind(capa.heightProperty().subtract(ALTO_BANDA).divide(2));
        capa.getChildren().add(banda);

        String viejo = antes.etiqueta();
        String nuevo = despues.etiqueta();
        String corrupto = corromperTexto(viejo, 2) + " → " + corromperTexto(nuevo, 3);
        Timeline linea = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(banda.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(banda.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(700), e -> titulo.setText(corrupto)),
                new KeyFrame(Duration.millis(1000), e -> titulo.setText(nuevo)),
                new KeyFrame(Duration.millis(1400), new KeyValue(banda.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(1700), new KeyValue(banda.opacityProperty(), 0)));
        linea.setOnFinished(e -> {
            banda.prefWidthProperty().unbind();
            banda.layoutYProperty().unbind();
            capa.getChildren().remove(banda);
        });
        linea.play();
    }

    public void abandonar(ItemConsumption item) {
        Sonidos.abandonar();
        if (!Efectos.accionesPermitidas()) return;
        esperarNodo(item, 0, this::bandaAbandono);
    }

    private void bandaAbandono(Bounds zona) {
        boolean fila = zona.getWidth() > zona.getHeight() * 3;
        double alto = fila ? zona.getHeight() : ALTO_BANDA_TARJETA;
        Text titulo = new Text("ABANDONADO");
        titulo.getStyleClass().add("efecto-abandono-titulo");
        Text subtitulo = new Text("> THE MAW SUELTA LA PRESA");
        subtitulo.getStyleClass().add("efecto-banda-sub");
        subtitulo.setOpacity(0);
        Pane banda;
        if (fila) {
            HBox horizontal = new HBox(14, titulo, subtitulo);
            horizontal.setAlignment(Pos.CENTER);
            banda = horizontal;
        } else {
            VBox vertical = new VBox(2, titulo, subtitulo);
            vertical.setAlignment(Pos.CENTER);
            banda = vertical;
        }
        banda.getStyleClass().add("efecto-abandono");
        if (!fila) banda.getStyleClass().add("efecto-abandono-tarjeta");
        banda.setMinSize(zona.getWidth(), alto);
        banda.setPrefSize(zona.getWidth(), alto);
        banda.setMaxSize(zona.getWidth(), alto);
        banda.setLayoutX(zona.getMinX());
        banda.setLayoutY(fila ? zona.getMinY() : zona.getMinY() + (zona.getHeight() - alto) / 2);
        banda.setOpacity(0);
        capa.getChildren().add(banda);
        Timeline linea = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(banda.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(banda.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(500), e -> titulo.setText("AB▒NDONADO"),
                        new KeyValue(banda.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(900), e -> {
                    titulo.setText("ABANDONADO");
                    subtitulo.setOpacity(1);
                }),
                new KeyFrame(Duration.millis(1700), new KeyValue(banda.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(2100), new KeyValue(banda.opacityProperty(), 0)));
        linea.setOnFinished(e -> capa.getChildren().remove(banda));
        linea.play();
    }

    private static String corromperTexto(String texto, int posicion) {
        if (texto.length() <= posicion) return texto;
        return texto.substring(0, posicion) + "▒" + texto.substring(posicion + 1);
    }

    private void barrido(Bounds zona) {
        Rectangle consumida = new Rectangle(zona.getMinX(), zona.getMinY(), 0, zona.getHeight());
        consumida.getStyleClass().add("efecto-consumido");
        Rectangle linea = new Rectangle(zona.getMinX(), zona.getMinY(), 3, zona.getHeight());
        linea.getStyleClass().add("efecto-linea-mordida");
        capa.getChildren().addAll(consumida, linea);
        Timeline barrido = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(consumida.widthProperty(), 0),
                        new KeyValue(linea.xProperty(), zona.getMinX())),
                new KeyFrame(Duration.millis(550),
                        new KeyValue(consumida.widthProperty(), zona.getWidth()),
                        new KeyValue(linea.xProperty(), zona.getMaxX() - 3),
                        new KeyValue(consumida.opacityProperty(), 1),
                        new KeyValue(linea.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(800),
                        new KeyValue(consumida.opacityProperty(), 0),
                        new KeyValue(linea.opacityProperty(), 0)));
        barrido.setOnFinished(e -> capa.getChildren().removeAll(consumida, linea));
        barrido.play();
    }

    private void parpadeo(Bounds zona) {
        Rectangle fondo = new Rectangle(zona.getMinX(), zona.getMinY(), zona.getWidth(), zona.getHeight());
        fondo.getStyleClass().add("efecto-resalte");
        Rectangle franja = new Rectangle(zona.getMinX(), zona.getMinY(), 3, zona.getHeight());
        franja.getStyleClass().add("efecto-linea-mordida");
        fondo.setOpacity(0);
        franja.setOpacity(0);
        capa.getChildren().addAll(fondo, franja);
        Timeline luz = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fondo.opacityProperty(), 0),
                        new KeyValue(franja.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(150), new KeyValue(fondo.opacityProperty(), 0.35),
                        new KeyValue(franja.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(500), new KeyValue(fondo.opacityProperty(), 0.08)),
                new KeyFrame(Duration.millis(750), new KeyValue(fondo.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(1200), new KeyValue(fondo.opacityProperty(), 0),
                        new KeyValue(franja.opacityProperty(), 0)));
        luz.setOnFinished(e -> capa.getChildren().removeAll(fondo, franja));
        luz.play();
    }

    private void esperarNodo(ItemConsumption item, int intento, Consumer<Bounds> alEncontrar) {
        PauseTransition espera = new PauseTransition(Duration.millis(intento == 0 ? 60 : 80));
        espera.setOnFinished(e -> {
            if (intento == 0) desplazarHasta(item);
            Bounds zona = zonaDe(buscarNodo(item));
            if (zona != null) alEncontrar.accept(zona);
            else if (intento < INTENTOS) esperarNodo(item, intento + 1, alEncontrar);
        });
        espera.play();
    }

    private void desplazarHasta(ItemConsumption item) {
        Node nodo = contenido.lookup(".table-view");
        if (nodo instanceof TableView<?> tabla) {
            int indice = tabla.getItems().indexOf(item);
            if (indice >= 0) tabla.scrollTo(Math.max(0, indice - 2));
        } else {
            desplazarEnScroll(item);
        }
    }

    private void desplazarEnScroll(ItemConsumption item) {
        Node destino = buscarNodo(item);
        if (destino == null) return;
        for (Node padre = destino.getParent(); padre != null; padre = padre.getParent()) {
            if (padre instanceof ScrollPane panel && panel.getContent() != null) {
                Bounds zona = panel.getContent().sceneToLocal(destino.localToScene(destino.getBoundsInLocal()));
                if (zona == null) return;
                panel.setVvalue(ReglasAcciones.posicionDesplazamiento(zona.getMinY(), zona.getHeight(),
                        panel.getViewportBounds().getHeight(), panel.getContent().getLayoutBounds().getHeight()));
                return;
            }
        }
    }

    private Node buscarNodo(ItemConsumption item) {
        for (Node nodo : contenido.lookupAll(".table-row-cell")) {
            if (nodo instanceof TableRow<?> fila && !fila.isEmpty() && fila.getItem() == item) return fila;
        }
        return buscarTarjeta(contenido, item);
    }

    private Node buscarTarjeta(Node raiz, ItemConsumption item) {
        if (raiz instanceof TarjetaItem tarjeta && tarjeta.getUserData() == item) return tarjeta;
        if (raiz instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                Node encontrado = buscarTarjeta(hijo, item);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private Bounds zonaDe(Node nodo) {
        if (nodo == null || nodo.getScene() == null || !nodo.isVisible()) return null;
        Bounds zona = capa.sceneToLocal(nodo.localToScene(nodo.getBoundsInLocal()));
        if (zona == null || zona.getWidth() < 1 || zona.getHeight() < 1) return null;
        double minX = Math.max(zona.getMinX(), 0);
        double minY = Math.max(zona.getMinY(), 0);
        double maxX = Math.min(zona.getMaxX(), capa.getWidth());
        double maxY = Math.min(zona.getMaxY(), capa.getHeight());
        if (maxX - minX < 1 || maxY - minY < zona.getHeight() * 0.5) return null;
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
}
