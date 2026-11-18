package consumption.ui;

import consumption.data.Preferencias;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.util.List;

public final class DialogoOpciones {
    private DialogoOpciones() { }

    public static void mostrar(Window propietario, List<String> hojas,
                               Preferencias preferencias, Runnable refrescarPagina,
                               Runnable refrescarEscala, Runnable abrirPapelera,
                               Runnable abrirDatos, Runnable revisarPortadas) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle("THE MAW — OPCIONES");
        dialogo.setHeaderText("OPCIONES // THE MAW");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label visual = new Label("// VISUALIZACION");
        visual.getStyleClass().add("opciones-seccion");
        CheckBox tabla = new CheckBox("TABLA COMPACTA");
        tabla.setSelected(preferencias.isTablaCompacta());
        tabla.setOnAction(e -> {
            preferencias.setTablaCompacta(tabla.isSelected());
            preferencias.guardar();
            refrescarPagina.run();
        });
        CheckBox tarjetas = new CheckBox("TARJETAS COMPACTAS EN INICIO");
        tarjetas.setSelected(preferencias.isTarjetasCompactas());
        tarjetas.setOnAction(e -> {
            preferencias.setTarjetasCompactas(tarjetas.isSelected());
            preferencias.guardar();
            refrescarPagina.run();
        });

        Label texto = new Label("TAMAÑO GENERAL DEL TEXTO");
        Label porcentaje = new Label(preferencias.getEscalaTexto() + " %");
        porcentaje.getStyleClass().add("opciones-valor");
        Slider escala = new Slider(90, 115, preferencias.getEscalaTexto());
        escala.setMajorTickUnit(5);
        escala.setMinorTickCount(0);
        escala.setSnapToTicks(true);
        escala.setShowTickMarks(true);
        escala.setShowTickLabels(true);
        escala.setBlockIncrement(5);
        escala.valueProperty().addListener((obs, anterior, nuevo) -> {
            int valor = (int) Math.round(nuevo.doubleValue() / 5.0) * 5;
            if (valor == preferencias.getEscalaTexto()) return;
            preferencias.setEscalaTexto(valor);
            porcentaje.setText(valor + " %");
            preferencias.guardar();
            refrescarEscala.run();
            refrescarPagina.run();
            if (dialogo.getDialogPane().getScene() != null) {
                dialogo.getDialogPane().getStylesheets().setAll(
                        ((Scene) propietario.getScene()).getStylesheets());
            }
        });
        Label explicacion = new Label("100 % = TAMAÑO ORIGINAL · CAMBIOS EN TIEMPO REAL");
        explicacion.getStyleClass().add("subtitulo");
        explicacion.setWrapText(true);
        HBox filaTexto = new HBox(12, texto, porcentaje);
        filaTexto.setAlignment(Pos.CENTER_LEFT);
        Region flexible = new Region();
        HBox.setHgrow(flexible, Priority.ALWAYS);
        filaTexto.getChildren().add(1, flexible);

        Label mantenimiento = new Label("// DATOS Y MANTENIMIENTO");
        mantenimiento.getStyleClass().add("opciones-seccion");
        Button papelera = accion("PAPELERA", dialogo, abrirPapelera);
        Button carpeta = accion("ABRIR CARPETA DE DATOS", dialogo, abrirDatos);
        Button portadas = accion("REVISAR PORTADAS HUERFANAS", dialogo, revisarPortadas);
        FlowPane herramientas = new FlowPane(8, 8, papelera, carpeta, portadas);
        herramientas.setAlignment(Pos.CENTER_LEFT);

        VBox cuerpo = new VBox(13, visual, tabla, tarjetas, filaTexto, escala,
                explicacion, new Separator(), mantenimiento, herramientas);
        cuerpo.setPadding(new Insets(14));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(540);
        scroll.setPrefViewportHeight(330);
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        Terminal.dialogo(dialogo, "OPCIONES // THE MAW", null, null);
        Button cerrar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (cerrar != null) {
            cerrar.getStyleClass().add("opciones-cerrar-fijo");
            cerrar.setMinWidth(140);
            cerrar.setPrefWidth(140);
            cerrar.setMaxWidth(140);
        }
        dialogo.showAndWait();
    }

    private static Button accion(String nombre, Dialog<?> dialogo, Runnable accion) {
        Button boton = Terminal.corchetes(new Button(nombre));
        boton.getStyleClass().addAll("pestana", "boton-accion");
        boton.setOnAction(e -> {
            dialogo.close();
            accion.run();
        });
        return boton;
    }
}