package consumption.ui;

import consumption.data.Preferencias;
import consumption.data.RepositorioJson;
import javafx.animation.*;
import javafx.util.Duration;
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
                               Runnable abrirDatos, Runnable revisarPortadas,
                               Runnable crearRespaldo, Runnable restaurarRespaldo,
                               RepositorioJson repositorio) {
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
            boolean anterior = preferencias.isTablaCompacta();
            try {
                preferencias.setTablaCompacta(tabla.isSelected());
                preferencias.guardar();
                refrescarPagina.run();
            } catch (RuntimeException ex) {
                preferencias.setTablaCompacta(anterior);
                tabla.setSelected(anterior);
                errorGuardado(propietario, hojas, ex);
            }
        });
        CheckBox tarjetas = new CheckBox("TARJETAS COMPACTAS EN INICIO");
        tarjetas.setSelected(preferencias.isTarjetasCompactas());
        tarjetas.setOnAction(e -> {
            boolean anterior = preferencias.isTarjetasCompactas();
            try {
                preferencias.setTarjetasCompactas(tarjetas.isSelected());
                preferencias.guardar();
                refrescarPagina.run();
            } catch (RuntimeException ex) {
                preferencias.setTarjetasCompactas(anterior);
                tarjetas.setSelected(anterior);
                errorGuardado(propietario, hojas, ex);
            }
        });

        Label texto = new Label("TAMAÑO GENERAL DEL TEXTO");
        Label porcentaje = new Label(preferencias.getEscalaTexto() + " %");
        porcentaje.getStyleClass().add("opciones-valor");
        javafx.scene.effect.DropShadow brilloEscala = new javafx.scene.effect.DropShadow();
        brilloEscala.setRadius(9);
        brilloEscala.setSpread(0.12);
        porcentaje.setEffect(brilloEscala);
        PauseTransition limpiarGlitch = new PauseTransition(Duration.millis(250));
        limpiarGlitch.setOnFinished(e -> {
            porcentaje.setTranslateX(0);
            porcentaje.setOpacity(1);
            porcentaje.setEffect(null);
            porcentaje.getStyleClass().remove("opciones-valor-glitch-c30");
        });
        final Timeline[] glitchActual = {null};
        Slider escala = new Slider(90, 115, preferencias.getEscalaTexto());
        escala.setMajorTickUnit(5);
        escala.setMinorTickCount(0);
        escala.setSnapToTicks(true);
        escala.setShowTickMarks(true);
        escala.setShowTickLabels(false);
        escala.setBlockIncrement(5);
        final boolean[] revirtiendoEscala = {false};
        escala.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (revirtiendoEscala[0]) return;
            int valor = (int) Math.round(nuevo.doubleValue() / 5.0) * 5;
            if (valor == preferencias.getEscalaTexto()) return;
            int previo = preferencias.getEscalaTexto();
            try {
                preferencias.setEscalaTexto(valor);
                preferencias.guardar();
            } catch (RuntimeException ex) {
                preferencias.setEscalaTexto(previo);
                revirtiendoEscala[0] = true;
                try { escala.setValue(previo); }
                finally { revirtiendoEscala[0] = false; }
                porcentaje.setText(previo + " %");
                errorGuardado(propietario, hojas, ex);
                return;
            }
            limpiarGlitch.stop();
            if (glitchActual[0] != null) glitchActual[0].stop();
            porcentaje.getStyleClass().remove("opciones-valor-glitch-c30");
            porcentaje.getStyleClass().add("opciones-valor-glitch-c30");
            porcentaje.setText(previo + " %");
            porcentaje.setOpacity(0.22);
            glitchActual[0] = new Timeline(
                    new KeyFrame(Duration.millis(35), e -> {
                        porcentaje.setText(valor + " %");
                        porcentaje.setTranslateX(-3);
                        porcentaje.setOpacity(1);
                    }),
                    new KeyFrame(Duration.millis(75), e -> porcentaje.setTranslateX(3)),
                    new KeyFrame(Duration.millis(115), e -> porcentaje.setTranslateX(-2)),
                    new KeyFrame(Duration.millis(165), e -> porcentaje.setTranslateX(2)),
                    new KeyFrame(Duration.millis(215), e -> porcentaje.setTranslateX(0))
            );
            glitchActual[0].playFromStart();
            limpiarGlitch.playFromStart();
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
        Button respaldar = accion("CREAR RESPALDO", dialogo, crearRespaldo);
        Button restaurar = accion("RESTAURAR RESPALDO", dialogo, restaurarRespaldo);
        restaurar.getStyleClass().add("boton-peligro");
        FlowPane herramientas = new FlowPane(8, 8, papelera, carpeta, portadas,
                respaldar, restaurar);
        herramientas.setAlignment(Pos.CENTER_LEFT);

        Label integridad = new Label("// INTEGRIDAD DE DATOS");
        integridad.getStyleClass().add("opciones-seccion");
        int rechazados = repositorio.cantidadRechazados();
        Label estado = new Label(rechazados == 0
                ? "SIN REGISTROS RECHAZADOS"
                : rechazados + " REGISTROS CONSERVADOS SIN CARGAR");
        estado.setWrapText(true);
        Label aclaracion = new Label("Los registros rechazados permanecen en datos.json. "
                + "Consultar o exportar no modifica el archivo original.");
        aclaracion.getStyleClass().add("subtitulo");
        aclaracion.setWrapText(true);
        Button detalles = Terminal.corchetes(new Button("VER MOTIVOS"));
        detalles.getStyleClass().addAll("pestana", "boton-accion");
        detalles.setDisable(rechazados == 0);
        detalles.setOnAction(e -> {
            Alert detalle = new Alert(Alert.AlertType.INFORMATION);
            detalle.initOwner(propietario);
            detalle.setTitle("THE MAW — INTEGRIDAD DE DATOS");
            detalle.setHeaderText("REGISTROS RECHAZADOS (SOLO LECTURA)");
            TextArea textoMotivos = new TextArea(String.join("\n", repositorio.getAvisosCarga()));
            textoMotivos.setEditable(false);
            textoMotivos.setWrapText(true);
            textoMotivos.setPrefColumnCount(65);
            textoMotivos.setPrefRowCount(12);
            detalle.getDialogPane().setContent(textoMotivos);
            detalle.getDialogPane().getStylesheets().setAll(hojas);
            detalle.showAndWait();
        });
        Button exportar = Terminal.corchetes(new Button("EXPORTAR JSON"));
        exportar.getStyleClass().addAll("pestana", "boton-accion");
        exportar.setDisable(rechazados == 0);
        exportar.setOnAction(e -> {
            javafx.stage.FileChooser selector = new javafx.stage.FileChooser();
            selector.setTitle("EXPORTAR REGISTROS RECHAZADOS");
            selector.setInitialFileName("registros-rechazados.json");
            selector.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
            java.io.File destino = selector.showSaveDialog(propietario);
            if (destino == null) return;
            try {
                repositorio.exportarRechazados(destino.toPath());
                Alert exito = new Alert(Alert.AlertType.INFORMATION,
                        "Copia exportada. datos.json no fue modificado.", ButtonType.OK);
                exito.initOwner(propietario);
                exito.getDialogPane().getStylesheets().setAll(hojas);
                exito.showAndWait();
            } catch (RuntimeException ex) {
                Alert error = new Alert(Alert.AlertType.ERROR,
                        "No se pudo exportar. No se modificaron los registros: "
                                + ex.getMessage(), ButtonType.OK);
                error.initOwner(propietario);
                error.getDialogPane().getStylesheets().setAll(hojas);
                error.showAndWait();
            }
        });
        HBox accionesIntegridad = new HBox(8, detalles, exportar);
        accionesIntegridad.setAlignment(Pos.CENTER_LEFT);

        HBox contenedorEscala = new HBox(escala);
        contenedorEscala.setPadding(new Insets(3, 8, 0, 8));
        HBox.setHgrow(escala, Priority.ALWAYS);
        escala.setMaxWidth(Double.MAX_VALUE);
        HBox marcasEscala = new HBox();
        marcasEscala.setAlignment(Pos.CENTER_LEFT);
        marcasEscala.setPadding(new Insets(0, 8, 0, 8));
        for (int valorMarca = 90; valorMarca <= 115; valorMarca += 5) {
            Label marca = new Label(Integer.toString(valorMarca));
            marca.getStyleClass().add("subtitulo");
            marca.setMinWidth(Region.USE_PREF_SIZE);
            if (valorMarca == 90) {
                marcasEscala.getChildren().add(marca);
            } else {
                Region espacio = new Region();
                HBox.setHgrow(espacio, Priority.ALWAYS);
                marcasEscala.getChildren().addAll(espacio, marca);
            }
        }
        VBox controlEscala = new VBox(2, contenedorEscala, marcasEscala);
        controlEscala.setMinWidth(0);
        VBox cuerpo = new VBox(13, visual, tabla, tarjetas, filaTexto, controlEscala,
                explicacion, new Separator(), mantenimiento, herramientas,
                new Separator(), integridad, estado, aclaracion, accionesIntegridad);
        cuerpo.setPadding(new Insets(14));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(540);
        scroll.setPrefViewportHeight(330);
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        Terminal.dialogo(dialogo, "OPCIONES // THE MAW", null, null);
        Terminal.prepararCentrado(dialogo);
        Button cerrar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (cerrar != null) {
            cerrar.getStyleClass().add("opciones-cerrar-fijo");
            cerrar.setMinWidth(140);
            cerrar.setPrefWidth(140);
            cerrar.setMaxWidth(140);
        }
        dialogo.showAndWait();
    }

    private static void errorGuardado(Window propietario, List<String> hojas, RuntimeException ex) {
        Alert error = new Alert(Alert.AlertType.ERROR,
                "No se guardaron los cambios de OPCIONES. Se ha restaurado el valor anterior. "
                        + ex.getMessage(), ButtonType.CLOSE);
        error.initOwner(propietario);
        error.setTitle("THE MAW — ERROR DE PREFERENCIAS");
        error.setHeaderText(null);
        error.getDialogPane().getStylesheets().setAll(hojas);
        error.showAndWait();
    }

    private static Button accion(String nombre, Dialog<?> dialogo, Runnable accion) {
        Button boton = Terminal.corchetes(new Button(nombre));
        boton.getStyleClass().addAll("pestana", "boton-accion");
        boton.setOnAction(e -> {
            dialogo.close();
            javafx.application.Platform.runLater(accion);
        });
        return boton;
    }
}