package consumption.ui;

import consumption.data.Preferencias;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public final class GeometriaVentana {

    public static final double MIN_ANCHO = 960;
    public static final double MIN_ALTO = 540;

    private GeometriaVentana() {
    }

    public static void aplicar(Stage stage, Preferencias prefs) {
        Rectangle2D visible = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(MIN_ANCHO, visible.getWidth()));
        stage.setMinHeight(Math.min(MIN_ALTO, visible.getHeight()));

        if (prefs.isVentanaGuardada()) {
            double ancho = Math.min(visible.getWidth(), Math.max(stage.getMinWidth(), prefs.getVentanaAncho()));
            double alto = Math.min(visible.getHeight(), Math.max(stage.getMinHeight(), prefs.getVentanaAlto()));
            stage.setWidth(ancho);
            stage.setHeight(alto);
            if (ReglasVentana.tituloVisible(prefs.getVentanaX(), prefs.getVentanaY(), ancho, pantallas())) {
                stage.setX(prefs.getVentanaX());
                stage.setY(prefs.getVentanaY());
            } else {
                stage.centerOnScreen();
            }
        }
        stage.setMaximized(true);
    }

    public static void registrar(Stage stage, Preferencias prefs) {
        Runnable anotar = () -> Platform.runLater(() -> {
            if (stage.isShowing() && !stage.isMaximized() && !stage.isIconified()) {
                prefs.setVentana(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            }
        });
        stage.xProperty().addListener((o, a, b) -> anotar.run());
        stage.yProperty().addListener((o, a, b) -> anotar.run());
        stage.widthProperty().addListener((o, a, b) -> anotar.run());
        stage.heightProperty().addListener((o, a, b) -> anotar.run());
        anotar.run();
    }

    private static List<double[]> pantallas() {
        List<double[]> lista = new ArrayList<>();
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            lista.add(new double[]{b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight()});
        }
        return lista;
    }
}