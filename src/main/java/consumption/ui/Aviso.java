package consumption.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class Aviso extends StackPane {

    private static final Duration ESPERA_NORMAL = Duration.seconds(2.2);
    private static final Duration ESPERA_DESHACER = Duration.seconds(ReglasAcciones.SEGUNDOS_DESHACER);

    private final Label texto = new Label();
    private final Button deshacer = new Button("DESHACER");
    private final HBox caja = new HBox(texto, deshacer);
    private SequentialTransition animacion;
    private Runnable alDeshacer;
    private Runnable alConfirmar;

    public Aviso() {
        setPickOnBounds(false);
        caja.setPickOnBounds(false);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        caja.setVisible(false);

        texto.getStyleClass().add("aviso-maw");
        texto.setMouseTransparent(true);

        deshacer.getStyleClass().add("aviso-boton");
        deshacer.setFocusTraversable(false);
        deshacer.setOnAction(e -> pulsoDeshacer());

        StackPane.setAlignment(caja, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(caja, new Insets(0, 8, 8, 0));
        getChildren().add(caja);
    }

    public void mostrar(String mensaje) {
        confirmarPendiente();
        presentar(mensaje, ESPERA_NORMAL, null, null);
    }

    public void mostrarConDeshacer(String mensaje, Runnable alDeshacer, Runnable alConfirmar) {
        confirmarPendiente();
        presentar(mensaje, ESPERA_DESHACER, alDeshacer, alConfirmar);
    }

    public void confirmarPendiente() {
        Runnable accion = alConfirmar;
        alConfirmar = null;
        alDeshacer = null;
        if (accion != null) accion.run();
    }

    private void presentar(String mensaje, Duration duracion, Runnable alDeshacer, Runnable alConfirmar) {
        if (animacion != null) animacion.stop();
        this.alDeshacer = alDeshacer;
        this.alConfirmar = alConfirmar;

        texto.setText("[ OK ] " + mensaje);
        boolean conBoton = alDeshacer != null;
        deshacer.setVisible(conBoton);
        deshacer.setManaged(conBoton);
        caja.setOpacity(0);
        caja.setVisible(true);

        FadeTransition entra = new FadeTransition(Duration.millis(180), caja);
        entra.setToValue(1);
        PauseTransition espera = new PauseTransition(duracion);
        FadeTransition sale = new FadeTransition(Duration.millis(450), caja);
        sale.setToValue(0);

        animacion = new SequentialTransition(entra, espera, sale);
        animacion.setOnFinished(e -> {
            caja.setVisible(false);
            confirmarPendiente();
        });
        animacion.play();
    }

    private void pulsoDeshacer() {
        Runnable accion = alDeshacer;
        alDeshacer = null;
        alConfirmar = null;
        if (animacion != null) animacion.stop();
        caja.setVisible(false);
        if (accion != null) accion.run();
    }
}