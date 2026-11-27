package consumption.ui;

import consumption.data.RepositorioJson;
import consumption.model.ItemConsumption;
import consumption.model.Numeracion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;

public final class DialogoRechazados {
    private DialogoRechazados() { }

    public static void mostrar(Window propietario, List<String> hojas, RepositorioJson repositorio,
                               List<ItemConsumption> items, Runnable alCambiar) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle("THE MAW — INTEGRIDAD DE DATOS");
        dialogo.setHeaderText("REGISTROS RECHAZADOS");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<RepositorioJson.Rechazado> lista = new ListView<>();
        lista.setPrefWidth(240);
        lista.setMinWidth(200);
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(RepositorioJson.Rechazado r, boolean vacio) {
                super.updateItem(r, vacio);
                setText(vacio || r == null ? null
                        : r.getCategoria().toUpperCase() + " // " + r.getResumen());
            }
        });

        Label motivo = new Label();
        motivo.setWrapText(true);
        Label guia = new Label("Corrige el JSON y pulsa RECUPERAR, o descarta el registro. "
                + "Al descartar se guarda antes una copia en la carpeta de datos.");
        guia.getStyleClass().add("subtitulo");
        guia.setWrapText(true);
        TextArea editor = new TextArea();
        editor.setWrapText(false);
        editor.setPrefRowCount(14);
        editor.setPrefColumnCount(48);
        VBox.setVgrow(editor, Priority.ALWAYS);
        Label estado = new Label();
        estado.setWrapText(true);

        Button recuperar = Terminal.corchetes(new Button("RECUPERAR"));
        recuperar.getStyleClass().add("a2-button");
        recuperar.getStyleClass().addAll("pestana", "boton-accion");
        Button descartar = Terminal.corchetes(new Button("DESCARTAR"));
        descartar.getStyleClass().add("a2-button");
        descartar.getStyleClass().addAll("pestana", "boton-accion", "boton-peligro");
        HBox acciones = new HBox(8, recuperar, descartar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        Runnable refrescar = () -> {
            lista.getItems().setAll(repositorio.getRechazados());
            boolean hay = !lista.getItems().isEmpty();
            recuperar.setDisable(!hay);
            descartar.setDisable(!hay);
            editor.setDisable(!hay);
            if (hay) {
                lista.getSelectionModel().selectFirst();
            } else {
                editor.clear();
                motivo.setText("NO QUEDAN REGISTROS RECHAZADOS");
            }
        };
        lista.getSelectionModel().selectedItemProperty().addListener((obs, antes, r) -> {
            if (r == null) return;
            editor.setText(r.getTexto());
            motivo.setText("MOTIVO: " + r.getMotivo());
        });

        recuperar.setOnAction(e -> {
            RepositorioJson.Rechazado r = lista.getSelectionModel().getSelectedItem();
            if (r == null) return;
            ItemConsumption item;
            try {
                item = repositorio.interpretar(r, editor.getText());
            } catch (IllegalArgumentException ex) {
                estado.setText("!! NO VALIDO: " + ex.getMessage());
                return;
            }
            boolean repetido = items.stream().anyMatch(i -> i.getNumero() == item.getNumero());
            if (item.getNumero() <= 0 || repetido) item.setNumero(Numeracion.siguiente(items));
            repositorio.quitarRechazado(r);
            items.add(item);
            try {
                repositorio.guardar(items);
            } catch (RuntimeException ex) {
                items.remove(item);
                repositorio.reponerRechazado(r);
                estado.setText("!! NO SE GUARDO: " + ex.getMessage());
                return;
            }
            alCambiar.run();
            refrescar.run();
            estado.setText("[ OK ] RECUPERADO: " + item.getTitulo());
        });

        descartar.setOnAction(e -> {
            RepositorioJson.Rechazado r = lista.getSelectionModel().getSelectedItem();
            if (r == null) return;
            Alert pregunta = new Alert(Alert.AlertType.CONFIRMATION,
                    "Se guardara una copia del registro y se quitara de datos.json. "
                            + "Esto no se puede deshacer desde la app.",
                    ButtonType.OK, ButtonType.CANCEL);
            pregunta.initOwner(dialogo.getDialogPane().getScene().getWindow());
            pregunta.setTitle("THE MAW — DESCARTAR REGISTRO");
            pregunta.setHeaderText("DESCARTAR REGISTRO RECHAZADO");
            pregunta.getDialogPane().getStylesheets().setAll(hojas);
            Terminal.dialogo(pregunta, "DESCARTAR REGISTRO RECHAZADO", "boton-peligro", "DESCARTAR");
            if (pregunta.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
            Path copia;
            try {
                copia = repositorio.guardarCopiaDescartado(r);
            } catch (RuntimeException ex) {
                estado.setText("!! NO SE DESCARTO (sin copia): " + ex.getMessage());
                return;
            }
            repositorio.quitarRechazado(r);
            try {
                repositorio.guardar(items);
            } catch (RuntimeException ex) {
                repositorio.reponerRechazado(r);
                estado.setText("!! NO SE GUARDO: " + ex.getMessage());
                return;
            }
            refrescar.run();
            estado.setText("[ OK ] DESCARTADO. COPIA: " + copia.getFileName());
        });

        VBox derecha = new VBox(8, motivo, guia, editor, acciones, estado);
        HBox.setHgrow(derecha, Priority.ALWAYS);
        HBox cuerpo = new HBox(12, lista, derecha);
        cuerpo.setPadding(new Insets(12));
        cuerpo.setPrefWidth(820);
        cuerpo.setPrefHeight(440);
        dialogo.getDialogPane().setContent(cuerpo);
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        Terminal.dialogo(dialogo, "REGISTROS RECHAZADOS // THE MAW", null, null);
        refrescar.run();
        dialogo.showAndWait();
    }
}
