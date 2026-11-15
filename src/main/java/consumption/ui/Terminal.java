package consumption.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public final class Terminal {

    private Terminal() {
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