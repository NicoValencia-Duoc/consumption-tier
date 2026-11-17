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

public final class Terminal {

    private Terminal() {
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
        for (ButtonType tipo : panel.getButtonTypes()) {
            Node nodo = panel.lookupButton(tipo);
            if (!(nodo instanceof Button boton)) continue;

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
        panel.setPrefWidth(Math.max(panel.getPrefWidth(), 470));
        panel.setMinHeight(Region.USE_COMPUTED_SIZE);
        panel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialogo.addEventHandler(javafx.scene.control.DialogEvent.DIALOG_SHOWN, e -> {
            panel.applyCss();
            panel.layout();
            Window ventana = panel.getScene() == null ? null : panel.getScene().getWindow();
            if (ventana instanceof javafx.stage.Stage stage) {
                Rectangle2D limites = Screen.getScreensForRectangle(
                                stage.getX(), stage.getY(), Math.max(1,stage.getWidth()), Math.max(1,stage.getHeight()))
                        .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
                double ancho = Math.min(limites.getWidth(), Math.max(470, panel.prefWidth(-1)));
                if (dialogo instanceof Alert alerta) {
                    String mensaje = alerta.getContentText() == null ? "" : alerta.getContentText();
                    String cabecera = alerta.getHeaderText() == null ? "" : alerta.getHeaderText();
                    int lineas = 0;
                    for (String linea : mensaje.split("\\n", -1)) {
                        lineas += Math.max(1, (linea.length() + 53) / 54);
                    }
                    int lineasCabecera = Math.max(1, (cabecera.length() + 39) / 40);
                    double altoContenido = 150 + 22 * lineas + 23 * Math.max(0, lineasCabecera - 1);
                    double alto = Math.min(limites.getHeight(), Math.max(230, altoContenido));
                    stage.setMinHeight(0);
                    stage.setMinWidth(Math.min(420, limites.getWidth()));
                    stage.setWidth(ancho);
                    stage.setHeight(alto);
                } else {
                    stage.sizeToScene();
                    stage.setWidth(ancho);
                    stage.setHeight(Math.min(limites.getHeight(), stage.getHeight()));
                    stage.setMinWidth(Math.min(420, limites.getWidth()));
                    stage.setMinHeight(0);
                }
            }
        });
    }
}