package consumption.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.List;

final class Avisos {
    private Avisos() { }

    static void mostrar(List<String> hojas, Alert.AlertType tipo, String texto) {
        Alert alerta = new Alert(tipo, texto, ButtonType.OK);
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(hojas);
        Terminal.dialogo(alerta, tipo == Alert.AlertType.ERROR ? "!! ERROR" : "AVISO", "boton-accion", null);
        alerta.showAndWait();
    }
}
