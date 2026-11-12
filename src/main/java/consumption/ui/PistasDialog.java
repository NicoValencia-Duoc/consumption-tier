package consumption.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class PistasDialog {

    private static final String AYUDA = "Doble clic en una canción para reproducirla";

    public static void mostrar(ReproductorMusica reproductor, List<String> hojas) {
        ListView<Pista> lista = new ListView<>();
        lista.getItems().setAll(reproductor.getPistas());
        lista.setPrefSize(480, 300);
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Pista pista, boolean vacio) {
                super.updateItem(pista, vacio);
                if (vacio || pista == null) {
                    setText(null);
                    return;
                }
                boolean suena = pista.equals(reproductor.getActual());
                setText((suena ? "♪  " : "     ") + pista.textoCompleto());
            }
        });

        Label detalle = new Label(AYUDA);
        detalle.getStyleClass().add("subtitulo");
        detalle.setWrapText(true);
        lista.getSelectionModel().selectedItemProperty().addListener((obs, antes, pista) ->
                detalle.setText(pista == null || pista.fuente().isBlank()
                        ? AYUDA
                        : "FUENTE / LICENCIA: " + pista.fuente()));

        lista.setOnMouseClicked(ev -> {
            Pista elegida = lista.getSelectionModel().getSelectedItem();
            if (ev.getClickCount() == 2 && elegida != null) {
                reproductor.reproducir(elegida);
                lista.refresh();
            }
        });

        VBox contenido = new VBox(10, lista, detalle);
        contenido.setPadding(new Insets(16));

        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("PISTAS");
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.showAndWait();
    }
}
