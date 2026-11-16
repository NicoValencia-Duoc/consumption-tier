package consumption.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        }
    }
}