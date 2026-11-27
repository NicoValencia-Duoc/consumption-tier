package consumption.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.stage.Window;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class Terminal {

    private Terminal() {
    }

    private static Stage ventanaPrincipal;

    public static void registrarVentanaPrincipal(Stage stage) {
        ventanaPrincipal = stage;
    }

    public static void prepararCentrado(Dialog<?> dialogo) {
        if (ventanaPrincipal == null) return;

        if (dialogo.getOwner() == null) {
            dialogo.initOwner(ventanaPrincipal);
        }

        String clave = "the-maw.centrado-instalado";
        if (Boolean.TRUE.equals(dialogo.getDialogPane().getProperties().get(clave))) {
            return;
        }
        dialogo.getDialogPane().getProperties().put(clave, Boolean.TRUE);

        dialogo.addEventHandler(
                javafx.scene.control.DialogEvent.DIALOG_SHOWN,
                evento -> javafx.application.Platform.runLater(() -> {
                    Window ventana = dialogo.getDialogPane().getScene() == null
                            ? null
                            : dialogo.getDialogPane().getScene().getWindow();

                    if (!(ventana instanceof Stage emergente)) return;
                    if (!ventanaPrincipal.isShowing()) return;

                    double ancho = emergente.getWidth();
                    double alto = emergente.getHeight();
                    if (ancho <= 0 || alto <= 0) return;

                    double centroX = ventanaPrincipal.getX()
                            + ventanaPrincipal.getWidth() / 2.0;
                    double centroY = ventanaPrincipal.getY()
                            + ventanaPrincipal.getHeight() / 2.0;

                    Rectangle2D visible = Screen.getScreensForRectangle(
                                    centroX, centroY, 1, 1)
                            .stream()
                            .findFirst()
                            .orElse(Screen.getPrimary())
                            .getVisualBounds();

                    double x = centroX - ancho / 2.0;
                    double y = centroY - alto / 2.0;

                    double maxX = visible.getMaxX() - ancho;
                    double maxY = visible.getMaxY() - alto;

                    emergente.setX(Math.max(visible.getMinX(),
                            Math.min(x, maxX)));
                    emergente.setY(Math.max(visible.getMinY(),
                            Math.min(y, maxY)));
                }));
    }

    public static HBox titulo(String texto, Label subtitulo) {
        Label prompt = new Label(">");
        prompt.getStyleClass().add("titulo-prompt");
        Label nombre = new Label(texto);
        nombre.getStyleClass().add("titulo-pagina");
        HBox caja = new HBox(10, prompt, nombre, new CursorTerminal());
        caja.setAlignment(Pos.BASELINE_LEFT);
        if (subtitulo != null) {
            subtitulo.getStyleClass().add("subtitulo");
            caja.getChildren().add(subtitulo);
        }
        Efectos.glitchAlPasar(nombre);
        return caja;
    }

    public static StackPane panel(String titulo, Node contenido) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("panel-titulo");
        StackPane.setAlignment(etiqueta, Pos.TOP_LEFT);
        StackPane.setMargin(etiqueta, new Insets(0, 0, 0, 12));

        VBox caja = new VBox(contenido);
        caja.getStyleClass().add("panel-ficha");
        StackPane.setMargin(caja, new Insets(8, 0, 0, 0));

        StackPane marco = new StackPane(caja, etiqueta);
        marco.setAlignment(Pos.TOP_LEFT);
        return marco;
    }

    public static <B extends ButtonBase> B corchetes(B boton) {
        boton.textProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && !ahora.isEmpty() && !ahora.startsWith("[ ")) {
                boton.setText("[ " + ahora + " ]");
            }
        });
        String actual = boton.getText();
        if (actual != null && !actual.isEmpty() && !actual.startsWith("[ ")) {
            boton.setText("[ " + actual + " ]");
        }
        return boton;
    }

    public static void dialogo(Dialog<?> dialogo, String titulo, String claseOk, String textoOk) {
        String actual = dialogo.getHeaderText();
        if (actual == null || actual.isBlank()) dialogo.setHeaderText(titulo);

        DialogPane panel = dialogo.getDialogPane();
        dialogo.setGraphic(null);
        for (ButtonType tipo : panel.getButtonTypes()) {
            Node nodo = panel.lookupButton(tipo);
            if (!(nodo instanceof Button boton)) continue;
            if (!boton.getStyleClass().contains("a2-button")) boton.getStyleClass().add("a2-button");

            if (!boton.getStyleClass().contains("boton-dialogo")) boton.getStyleClass().add("boton-dialogo");
            if (tipo == ButtonType.OK) boton.setText("ACEPTAR");
            else if (tipo == ButtonType.CANCEL) boton.setText("CANCELAR");
            else if (tipo == ButtonType.CLOSE) boton.setText("CERRAR");

            if (tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (textoOk != null) boton.setText(textoOk);
                if (claseOk != null) boton.getStyleClass().add(claseOk);
            }
            corchetes(boton);
            boton.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            boton.setMaxWidth(Double.MAX_VALUE);
        }
        panel.setMinWidth(420);
        panel.setPrefWidth(Math.max(panel.getPrefWidth(), 540));
        if (dialogo instanceof Alert alerta && alerta.getContentText() != null
                && !alerta.getContentText().isBlank()) {
            javafx.scene.text.Text contenido = new javafx.scene.text.Text(alerta.getContentText());
            contenido.setWrappingWidth(490);
            contenido.setStyle("-fx-fill: -c-tx;");
            panel.setContent(new javafx.scene.layout.VBox(contenido));
        }
        panel.setMinHeight(Region.USE_COMPUTED_SIZE);
        panel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        prepararCentrado(dialogo);
    }
}