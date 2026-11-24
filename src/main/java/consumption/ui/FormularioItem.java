package consumption.ui;

import consumption.data.GestorImagenes;
import consumption.model.Estado;
import consumption.model.Formato;
import consumption.model.ItemConsumption;
import consumption.model.Tier;
import consumption.data.BorradorFormulario;
import consumption.data.Rutas;

import javafx.scene.Parent;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.embed.swing.SwingFXUtils;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.scene.input.Clipboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class FormularioItem<T extends ItemConsumption> {

    public static class ErrorCampo extends IllegalArgumentException {
        private final transient Node campo;

        public ErrorCampo(Node campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }

        public Node getCampo() {
            return campo;
        }
    }

    @FunctionalInterface
    public interface ComprobadorDuplicado {
        boolean existe(String tipo, String titulo, ItemConsumption propio);
    }

    private static final Set<Estado> CON_AVANCE = EnumSet.of(Estado.EN_PROGRESO, Estado.TERMINADO, Estado.ABANDONADO);
    private static final Set<Estado> CON_TERMINO = EnumSet.of(Estado.TERMINADO, Estado.ABANDONADO);

    private static final String TITULO_NUEVO = "THE MAW TIENE HAMBRE";
    private static final String TITULO_EDICION = "REESCRIBIENDO EL REGISTRO";

    private record Fila(String etiqueta, Node campo, VBox caja, Label error, Set<Estado> visibleEn) {
    }

    private final VBox cuerpo = new VBox(14);
    private final ScrollPane scroll = new ScrollPane(cuerpo);
    private final Label mensaje = new Label();
    private final List<Fila> filas = new ArrayList<>();
    private VBox seccionActual;
    private VBox seccionProgreso;
    private VBox seccionCriterios;
    private boolean irACriterios;
    private T resultado;
    private T original;

    private final TextField titulo = new TextField();
    private final ToggleGroup grupoEstado = new ToggleGroup();
    private final Map<Estado, ToggleButton> chips = new EnumMap<>(Estado.class);
    private DeslizadorAvance avance;
    private final CampoFecha inicio = new CampoFecha("El inicio");
    private final CampoFecha termino = new CampoFecha("El termino");
    private final CheckBox sinInicio = new CheckBox("SIN REGISTRO");
    private final CheckBox sinTermino = new CheckBox("SIN REGISTRO");
    private final Map<String, FilaCriterio> criterios = new LinkedHashMap<>();
    private final HBox notaViva = new HBox(10);
    private final TextField repeticiones = new TextField("0");
    private final TextArea comentario = new TextArea();
    private final TextField conQuien = new TextField();

    private boolean modificado;
    private boolean descarteConfirmado;
    private boolean restaurandoBorrador;
    private boolean capturarBorrador;
    private final PauseTransition demoraBorrador = new PauseTransition(Duration.millis(400));
    private ComprobadorDuplicado comprobador;
    private Dialog<ButtonType> dialogo;
    private List<String> hojas;

    private File archivoNuevo;
    private Path imagenTemporalPortapapeles;
    private boolean quitarImagen;
    private String portadaNuevaPendiente;
    private final Label nombreImagen = new Label("SIN IMAGEN");
    private final ImageView vistaPrevia = new ImageView();
    private HBox filaPortada;

    protected abstract String nombreTipo();
    protected abstract List<String> nombresCriterios();
    protected abstract void agregarCamposPropios();
    protected abstract void cargarPropios(T item);
    protected abstract T crear(String titulo, double nota, Estado estado);
    protected abstract void aplicarPropios(T item);
    protected boolean permiteSuperarCien() {
        return false;
    }
    protected boolean avanceAutomaticoAlTerminar() {
        return true;
    }
    public void setComprobadorDuplicado(ComprobadorDuplicado c) {
        this.comprobador = c;
    }
    public void setIrACriterios(boolean valor) {
        this.irACriterios = valor;
    }

    public Optional<T> mostrar(T existente, List<String> hojas) {
        this.original = existente;
        this.hojas = hojas;

        nuevaSeccion("IDENTIDAD");
        agregarFila("TITULO", titulo);
        agregarFila("ESTADO", crearChipsEstado());

        seccionProgreso = nuevaSeccion("PROGRESO");
        Set<Estado> conAvanceManual = avanceAutomaticoAlTerminar()
                ? EnumSet.of(Estado.EN_PROGRESO, Estado.ABANDONADO) : CON_AVANCE;
        agregarFilaCondicional("AVANCE (%)", crearAvance(), conAvanceManual);
        agregarFilaCondicional("INICIO", conSinRegistro(inicio, sinInicio), CON_AVANCE);
        agregarFilaCondicional("TERMINO", conSinRegistro(termino, sinTermino), CON_TERMINO);
        repeticiones.setPrefColumnCount(5);
        agregarFilaCondicional("VECES CONSUMIDO", repeticiones, CON_AVANCE);
        termino.setMinimo(() -> inicio.valorProperty().get());
        inicio.valorProperty().addListener((obs, antes, ahora) -> { modificado = true; revisarFechas(); });
        termino.valorProperty().addListener((obs, antes, ahora) -> { modificado = true; revisarFechas(); });

        nuevaSeccion("DATOS DE " + nombreTipo());
        agregarCamposPropios();

        nuevaSeccion("PORTADA");
        filaPortada = crearPortada();
        agregarFila("IMAGEN", filaPortada);

        seccionCriterios = nuevaSeccion("CRITERIOS · 0 A 10 · N/A SI NO APLICA");
        for (String criterio : nombresCriterios()) {
            FilaCriterio fila = new FilaCriterio();
            fila.valorProperty().addListener((obs, antes, ahora) -> {
                modificado = true;
                actualizarNota();
            });
            criterios.put(criterio, fila);
            agregarFila(criterio, fila);
        }
        agregarFila("NOTA FINAL", notaViva);
        nuevaSeccion("COMENTARIO");
        conQuien.setPromptText("Opcional · por ejemplo, Mati");
        conQuien.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= ItemConsumption.MAX_CON_QUIEN ? cambio : null));
        agregarFila("CON QUIEN", conQuien);
        comentario.setWrapText(true);
        comentario.setPrefRowCount(4);
        comentario.setPromptText("Opcional · hasta " + ItemConsumption.MAX_COMENTARIO + " caracteres");
        comentario.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= ItemConsumption.MAX_COMENTARIO ? cambio : null));
        HBox.setHgrow(comentario, Priority.ALWAYS);
        agregarFila("COMENTARIO", comentario);

        Label contadorComentario = new Label();
        contadorComentario.getStyleClass().add("subtitulo");
        contadorComentario.setPadding(new Insets(0, 0, 0, 160));
        contadorComentario.textProperty().bind(
                comentario.textProperty().length().asString("%d/" + ItemConsumption.MAX_COMENTARIO));
        seccionActual.getChildren().add(contadorComentario);

        if (existente != null) cargar(existente);
        aplicarVisibilidad();
        actualizarNota();
        modificado = false;

        cuerpo.setPadding(new Insets(14));
        cuerpo.getStyleClass().add("cuerpo-form");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(700);
        scroll.setMinWidth(0);
        scroll.setMinHeight(0);
        scroll.setPrefViewportHeight(480);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        mensaje.getStyleClass().add("mensaje-error");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(680);
        VBox raiz = new VBox(8, scroll, mensaje);

        Label atajoGuardar = new Label("GUARDAR : ENTER");
        Label atajoCancelar = new Label("CANCELAR : ESC");

        atajoGuardar.getStyleClass().add("subtitulo");
        atajoCancelar.getStyleClass().add("subtitulo");

        atajoGuardar.setAlignment(Pos.CENTER);
        atajoCancelar.setAlignment(Pos.CENTER);

        HBox indicaciones = new HBox(10, atajoGuardar, atajoCancelar);
        indicaciones.setAlignment(Pos.CENTER_RIGHT);
        indicaciones.setMaxWidth(Double.MAX_VALUE);

        dialogo = new Dialog<>();
        dialogo.setDialogPane(new DialogPane() {
            @Override
            protected Node createButtonBar() {
                Node barraNativa = super.createButtonBar();

                barraNativa.setStyle("-fx-padding: 0 0 0 0;");
                indicaciones.setPadding(Insets.EMPTY);

                VBox pie = new VBox(0, indicaciones, barraNativa);
                pie.setFillWidth(true);

                return pie;
            }
        });
        dialogo.setTitle((existente == null ? "AGREGAR " : "EDITAR ") + nombreTipo());
        dialogo.setHeaderText((existente == null ? TITULO_NUEVO : TITULO_EDICION) + " · " + nombreTipo());
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        dialogo.getDialogPane().getStyleClass().add("formulario-item-dialogo");
        dialogo.getDialogPane().setContent(raiz);
        dialogo.setOnShowing(ev -> {
            javafx.stage.Screen pantalla = javafx.stage.Screen.getScreensForRectangle(
                            dialogo.getOwner() == null ? 0 : dialogo.getOwner().getX(),
                            dialogo.getOwner() == null ? 0 : dialogo.getOwner().getY(), 1, 1)
                    .stream().findFirst().orElse(javafx.stage.Screen.getPrimary());
            javafx.geometry.Rectangle2D visible = pantalla.getVisualBounds();
            scroll.setPrefViewportWidth(Math.min(700, Math.max(320, visible.getWidth() - 110)));
            scroll.setPrefViewportHeight(Math.min(480, Math.max(180, visible.getHeight() - 240)));
        });
        ButtonType guardar = new ButtonType("GUARDAR", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, cancelar);

        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        Button botonCancelar = (Button) dialogo.getDialogPane().lookupButton(cancelar);
        botonGuardar.getStyleClass().addAll("boton-dialogo", "boton-accion");
        botonCancelar.getStyleClass().add("boton-dialogo");
        Terminal.corchetes(botonGuardar);
        Terminal.corchetes(botonCancelar);
        atajoGuardar.prefWidthProperty().bind(botonGuardar.widthProperty());
        atajoCancelar.prefWidthProperty().bind(botonCancelar.widthProperty());

        botonGuardar.addEventFilter(ActionEvent.ACTION, ev -> {
            limpiarErrores();
            try {
                T construido = construir(existente);
                if (construido == null) {
                    ev.consume();
                } else {
                    resultado = construido;
                }
            } catch (ErrorCampo ex) {
                mostrarError(ex.getCampo(), ex.getMessage());
                ev.consume();
            } catch (IllegalArgumentException ex) {
                mostrarErrorDelModelo(ex.getMessage());
                ev.consume();
            }
        });

        botonCancelar.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!puedeDescartar()) ev.consume();
        });

        Platform.runLater(titulo::requestFocus);
        if (irACriterios) Platform.runLater(() -> Platform.runLater(this::desplazarACriterios));

        dialogo.setOnShown(ev -> {
            dialogo.getDialogPane().getScene().getWindow()
                    .addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, cierre -> {
                        if (!puedeDescartar()) cierre.consume();
                    });
            instalarBorrador();
        });

        Terminal.prepararCentrado(dialogo);
        Optional<ButtonType> respuesta;
        try {
            respuesta = dialogo.showAndWait();
        } finally {
            demoraBorrador.stop();
            if (!descarteConfirmado && capturarBorrador
                    && modificado && original == null) {
                try { BorradorFormulario.guardar(nombreTipo(), capturarCampos()); }
                catch (RuntimeException ex) {
                    mensaje.setText("!! BORRADOR NO GUARDADO: " + ex.getMessage());
                    System.err.println("Borrador pendiente: " + ex.getMessage());
                }
            }
            limpiarTemporalPortada();
            if (resultado == null) revertirPortadaNoGuardada();
        }
        return respuesta.filter(b -> b == guardar).map(b -> resultado);
    }

    public void confirmarPortadaGuardada() {
        portadaNuevaPendiente = null;
    }

    public void confirmarBorradorGuardado() {
        demoraBorrador.stop();
        capturarBorrador = false;
        if (original == null) {
            try {
                BorradorFormulario.borrar(nombreTipo());
            } catch (RuntimeException ex) {
                System.err.println("No se pudo borrar el borrador ya guardado: "
                        + ex.getMessage());
            }
        }
    }

    public void revertirPortadaNoGuardada() {
        if (portadaNuevaPendiente != null) {
            GestorImagenes.eliminar(portadaNuevaPendiente);
            portadaNuevaPendiente = null;
        }
    }

    private void instalarBorrador() {
        if (original != null) return;

        for (Fila fila : filas) {
            vigilarNodo(fila.campo());
        }
        inicio.valorProperty().addListener((o, a, b) -> guardarBorradorActual());
        termino.valorProperty().addListener((o, a, b) -> guardarBorradorActual());
        sinInicio.selectedProperty().addListener((o, a, b) -> guardarBorradorActual());
        sinTermino.selectedProperty().addListener((o, a, b) -> guardarBorradorActual());
        avance.valorProperty().addListener((o, a, b) -> guardarBorradorActual());
        grupoEstado.selectedToggleProperty().addListener((o, a, b) -> guardarBorradorActual());
        criterios.values().forEach(f ->
                f.valorProperty().addListener((o, a, b) -> guardarBorradorActual()));

        try {
            Optional<BorradorFormulario.Datos> previo = BorradorFormulario.leer(nombreTipo());
            if (previo.isPresent()) {
                boolean recuperar = preguntar(
                        "Hay un borrador de " + nombreTipo()
                                + " sin guardar. ¿Quieres recuperar lo escrito?",
                        "RECUPERAR", "EMPEZAR DE CERO");
                if (recuperar) {
                    restaurandoBorrador = true;
                    try {
                        aplicarBorrador(previo.get().campos);
                    } finally {
                        restaurandoBorrador = false;
                    }
                    modificado = true;
                } else {
                    BorradorFormulario.borrar(nombreTipo());
                }
            }
            capturarBorrador = true;
        } catch (RuntimeException ex) {
            Alert alerta = new Alert(Alert.AlertType.WARNING,
                    "No se pudo recuperar el borrador: " + ex.getMessage()
                            + "\nEl archivo se conserva para revisarlo; en esta sesion este formulario "
                            + "no guardara borradores para no pisarlo.",
                    ButtonType.OK);
            alerta.setHeaderText(null);
            alerta.getDialogPane().getStylesheets().setAll(hojas);
            alerta.showAndWait();
        }
    }

    private void vigilarNodo(Node nodo) {
        if (nodo instanceof TextInputControl entrada) {
            entrada.textProperty().addListener((o, a, b) -> guardarBorradorActual());
        } else if (nodo instanceof CheckBox casilla) {
            casilla.selectedProperty().addListener((o, a, b) -> guardarBorradorActual());
        }
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                vigilarNodo(hijo);
            }
        }
    }

    private void guardarBorradorActual() {
        if (!capturarBorrador || restaurandoBorrador || original != null) return;
        demoraBorrador.stop();
        demoraBorrador.setOnFinished(ev -> {
            if (!capturarBorrador || restaurandoBorrador || original != null) return;
            try {
                BorradorFormulario.guardar(nombreTipo(), capturarCampos());
            } catch (RuntimeException ex) {
                mensaje.setText("!! BORRADOR NO GUARDADO: " + ex.getMessage());
            }
        });
        demoraBorrador.playFromStart();
    }

    private Map<String, String> capturarCampos() {
        Map<String, String> datos = new LinkedHashMap<>();
        for (Fila fila : filas) {
            List<TextInputControl> entradas = new ArrayList<>();
            List<CheckBox> casillas = new ArrayList<>();
            buscarControles(fila.campo(), entradas, casillas);
            for (int i = 0; i < entradas.size(); i++) {
                datos.put(fila.etiqueta() + "#T" + i, entradas.get(i).getText());
            }
            for (int i = 0; i < casillas.size(); i++) {
                datos.put(fila.etiqueta() + "#C" + i,
                        Boolean.toString(casillas.get(i).isSelected()));
            }
        }
        datos.put("@ESTADO", estadoActual().name());
        datos.put("@AVANCE", Double.toString(avance.getValor()));
        for (Map.Entry<String, FilaCriterio> e : criterios.entrySet()) {
            Double valor = e.getValue().getValor();
            datos.put("@CRITERIO:" + e.getKey(),
                    valor == null ? "N/A" : Double.toString(valor));
        }
        return datos;
    }

    private void buscarControles(Node nodo, List<TextInputControl> textos,
                                 List<CheckBox> casillas) {
        if (nodo instanceof TextInputControl texto) textos.add(texto);
        if (nodo instanceof CheckBox casilla) casillas.add(casilla);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                buscarControles(hijo, textos, casillas);
            }
        }
    }

    private void aplicarBorrador(Map<String, String> datos) {
        String estado = datos.get("@ESTADO");
        if (estado != null) {
            try {
                chips.get(Estado.valueOf(estado)).setSelected(true);
            } catch (IllegalArgumentException ignorado) {
            }
        }

        for (Fila fila : filas) {
            List<TextInputControl> entradas = new ArrayList<>();
            List<CheckBox> casillas = new ArrayList<>();
            buscarControles(fila.campo(), entradas, casillas);
            for (int i = 0; i < entradas.size(); i++) {
                String valor = datos.get(fila.etiqueta() + "#T" + i);
                if (valor != null) entradas.get(i).setText(valor);
            }
            for (int i = 0; i < casillas.size(); i++) {
                String valor = datos.get(fila.etiqueta() + "#C" + i);
                if (valor != null) casillas.get(i).setSelected(Boolean.parseBoolean(valor));
            }
        }

        String porcentaje = datos.get("@AVANCE");
        if (porcentaje != null) {
            try {
                avance.setValor(Double.parseDouble(porcentaje));
            } catch (NumberFormatException ignorado) {
            }
        }

        for (Map.Entry<String, FilaCriterio> e : criterios.entrySet()) {
            String valor = datos.get("@CRITERIO:" + e.getKey());
            if (valor == null) continue;
            try {
                e.getValue().setValor("N/A".equals(valor)
                        ? null : Double.parseDouble(valor));
            } catch (NumberFormatException ignorado) {
            }
        }
        aplicarVisibilidad();
        actualizarNota();
    }

    private void borrarBorradorSiNuevo() {
        if (original == null) {
            BorradorFormulario.borrar(nombreTipo());
        }
    }

    private VBox nuevaSeccion(String nombre) {
        Label cabecera = new Label("// " + nombre);
        cabecera.getStyleClass().add("seccion-form");
        cabecera.setMaxWidth(Double.MAX_VALUE);
        Region linea = new Region();
        linea.getStyleClass().add("seccion-linea");

        HBox encabezado = new HBox(10, cabecera, linea);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.setFillHeight(false);
        HBox.setHgrow(linea, Priority.ALWAYS);
        linea.setPrefHeight(1);
        linea.setMaxHeight(1);
        VBox seccion = new VBox(8, encabezado);
        cuerpo.getChildren().add(seccion);
        seccionActual = seccion;
        return seccion;
    }

    protected void agregarFila(String etiqueta, Node campo) {
        registrarFila(etiqueta, campo, null);
    }

    private void agregarFilaCondicional(String etiqueta, Node campo, Set<Estado> visibleEn) {
        registrarFila(etiqueta, campo, visibleEn);
    }


    private void registrarFila(String etiqueta, Node campo, Set<Estado> visibleEn) {
        Label et = new Label(etiqueta);
        et.getStyleClass().add("etiqueta-form");
        et.setMinWidth(150);
        et.setPrefWidth(150);
        if (campo instanceof TextField campoTexto && campoTexto.getPrefColumnCount() == 12) {
            campoTexto.setPrefColumnCount(28);
        }

        HBox linea = new HBox(10, et, campo);
        linea.setAlignment(Pos.CENTER_LEFT);

        Label error = new Label();
        error.getStyleClass().add("error-campo");
        error.setWrapText(true);
        error.setPadding(new Insets(0, 0, 0, 160));
        error.setVisible(false);
        error.setManaged(false);

        VBox caja = new VBox(2, linea, error);
        seccionActual.getChildren().add(caja);

        Fila fila = new Fila(etiqueta, campo, caja, error, visibleEn);
        filas.add(fila);
        if (campo instanceof TextInputControl texto) {
            texto.textProperty().addListener((obs, antes, ahora) -> {
                modificado = true;
                limpiar(fila);
            });
        }
    }

    private Node crearChipsEstado() {
        FlowPane caja = new FlowPane(6, 6);
        for (Estado e : Estado.values()) {
            ToggleButton chip = new ToggleButton(e.getEtiqueta());
            chip.setToggleGroup(grupoEstado);
            chip.setUserData(e);
            Terminal.corchetes(chip);
            chip.getStyleClass().addAll("chip", e.getClaseCss().replace("estado-", "chip-"));
            chips.put(e, chip);
            caja.getChildren().add(chip);
        }
        grupoEstado.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                modificado = true;
                Estado previo = (antes == null) ? null : (Estado) antes.getUserData();
                Estado nuevo = (ahora == null) ? null : (Estado) ahora.getUserData();
                alCambiarEstado(previo, nuevo);
                aplicarVisibilidad();
            }
        });
        chips.get(Estado.PENDIENTE).setSelected(true);
        return caja;
    }
    private void alCambiarEstado(Estado previo, Estado nuevo) {
        if (nuevo == Estado.TERMINADO) {
            int veces;
            try {
                veces = Integer.parseInt(repeticiones.getText().trim());
            } catch (NumberFormatException ex) {
                veces = 0;
            }
            if (veces < 1) repeticiones.setText("1");
        }
        if (avance != null && previo == Estado.TERMINADO && nuevo == Estado.EN_PROGRESO
                && avanceAutomaticoAlTerminar()) {
            avance.setValor(0);
        }
    }

    private Node crearAvance() {
        avance = new DeslizadorAvance(permiteSuperarCien());
        avance.valorProperty().addListener((obs, antes, ahora) -> {
            modificado = true;
            limpiarDe(avance);
        });
        return avance;
    }

    private Node conSinRegistro(CampoFecha campo, CheckBox casilla) {
        casilla.getStyleClass().add("casilla-sin-registro");
        campo.disableProperty().bind(casilla.selectedProperty());
        casilla.selectedProperty().addListener((obs, antes, ahora) -> {
            modificado = true;
            if (ahora) campo.setValor(null);
            revisarFechas();
        });
        HBox caja = new HBox(14, campo, casilla);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private void limpiarTemporalPortada() {
        if (imagenTemporalPortapapeles == null) return;
        try {
            Files.deleteIfExists(imagenTemporalPortapapeles);
        } catch (IOException ex) {
            System.err.println("No se pudo eliminar portada temporal: " + ex.getMessage());
        }
        imagenTemporalPortapapeles = null;
    }

    private void pegarImagenDelPortapapeles(Image imagen) {
        if (imagen == null || imagen.isError()
                || imagen.getWidth() <= 0 || imagen.getHeight() <= 0) {
            throw new IllegalArgumentException("La imagen copiada no es valida.");
        }
        if (imagen.getWidth() > 6000 || imagen.getHeight() > 6000) {
            throw new IllegalArgumentException("La imagen supera 6000 pixeles por lado.");
        }

        Path temporal = null;
        try {
            BufferedImage buffered = SwingFXUtils.fromFXImage(imagen, null);
            if (buffered == null) {
                throw new IllegalArgumentException("No se pudo convertir la imagen copiada.");
            }
            temporal = Files.createTempFile("the-maw-portada-", ".png");
            if (!ImageIO.write(buffered, "png", temporal.toFile())) {
                throw new IOException("No se pudo codificar el PNG.");
            }

            GestorImagenes.validar(temporal.toFile());
            Path anterior = imagenTemporalPortapapeles;
            imagenTemporalPortapapeles = temporal;
            archivoNuevo = temporal.toFile();
            quitarImagen = false;
            nombreImagen.setText("IMAGEN PEGADA");
            refrescarVistaPrevia();
            modificado = true;

            if (anterior != null && !anterior.equals(temporal)) {
                try {
                    Files.deleteIfExists(anterior);
                } catch (IOException ex) {
                    System.err.println("No se pudo eliminar temporal anterior: " + ex.getMessage());
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            if (temporal != null && !temporal.equals(imagenTemporalPortapapeles)) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignorado) {}
            }
            throw new IllegalArgumentException("No se pudo pegar la imagen: " + ex.getMessage(), ex);
        }
    }

    private void usarArchivoPortada(File archivo) {
        GestorImagenes.validar(archivo);

        Image imagen = new Image(archivo.toURI().toString(), false);
        if (imagen.isError() || imagen.getWidth() <= 0 || imagen.getHeight() <= 0) {
            throw new IllegalArgumentException("El archivo no es una imagen valida.");
        }

        limpiarTemporalPortada();
        archivoNuevo = archivo;
        quitarImagen = false;
        nombreImagen.setText(archivo.getName());
        refrescarVistaPrevia();
        modificado = true;
    }

    private void abrirEditorEncuadre() {
        File archivo;
        if (archivoNuevo != null) {
            archivo = archivoNuevo;
        } else if (!quitarImagen && original != null
                && original.getRutaImagen() != null) {
            archivo = Rutas.IMAGENES.resolve(original.getRutaImagen()).toFile();
        } else {
            mostrarError(filaPortada, "Primero elige o pega una portada.");
            return;
        }

        if (!archivo.isFile()) {
            mostrarError(filaPortada, "No se encontro el archivo de portada.");
            return;
        }
        if (archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".gif")) {
            mostrarError(filaPortada, "El encuadre de GIF animados no esta disponible.");
            return;
        }

        Image fuente = new Image(archivo.toURI().toString(), false);
        if (fuente.isError() || fuente.getPixelReader() == null
                || fuente.getWidth() <= 0 || fuente.getHeight() <= 0) {
            mostrarError(filaPortada, "No se pudo abrir la portada para encuadrarla.");
            return;
        }

        final double proporcion = 104.0 / 134.0;
        final double anchoFuente = fuente.getWidth();
        final double altoFuente = fuente.getHeight();
        final double anchoBase = Math.min(anchoFuente, altoFuente * proporcion);
        final double altoBase = anchoBase / proporcion;

        ImageView vista = new ImageView(fuente);
        vista.setFitWidth(260);
        vista.setFitHeight(335);
        vista.setPreserveRatio(false);

        Slider zoom = new Slider(1, 3, 1);
        zoom.setShowTickLabels(true);
        zoom.setShowTickMarks(true);
        zoom.setMajorTickUnit(1);
        zoom.setBlockIncrement(0.1);
        zoom.setPrefWidth(260);

        final double[] posicion = {0.5, 0.5};
        final double[] ultimoRaton = {0, 0};
        final Runnable actualizar = () -> {
            double anchoRecorte = anchoBase / zoom.getValue();
            double altoRecorte = altoBase / zoom.getValue();
            double x = (anchoFuente - anchoRecorte) * posicion[0];
            double y = (altoFuente - altoRecorte) * posicion[1];
            vista.setViewport(new Rectangle2D(x, y, anchoRecorte, altoRecorte));
        };
        zoom.valueProperty().addListener((obs, antes, ahora) -> actualizar.run());
        vista.setOnMousePressed(e -> {
            ultimoRaton[0] = e.getX();
            ultimoRaton[1] = e.getY();
        });
        vista.setOnMouseDragged(e -> {
            double anchoRecorte = anchoBase / zoom.getValue();
            double altoRecorte = altoBase / zoom.getValue();
            double recorridoX = anchoFuente - anchoRecorte;
            double recorridoY = altoFuente - altoRecorte;
            if (recorridoX > 0) {
                posicion[0] = Math.max(0, Math.min(1,
                        posicion[0] - (e.getX() - ultimoRaton[0])
                                * anchoRecorte / 260.0 / recorridoX));
            }
            if (recorridoY > 0) {
                posicion[1] = Math.max(0, Math.min(1,
                        posicion[1] - (e.getY() - ultimoRaton[1])
                                * altoRecorte / 335.0 / recorridoY));
            }
            ultimoRaton[0] = e.getX();
            ultimoRaton[1] = e.getY();
            actualizar.run();
        });
        vista.setOnScroll(e -> {
            zoom.setValue(Math.max(zoom.getMin(),
                    Math.min(zoom.getMax(), zoom.getValue() + Math.signum(e.getDeltaY()) * 0.1)));
            e.consume();
        });
        actualizar.run();

        Button restablecer = Terminal.corchetes(new Button("RESTABLECER"));
        restablecer.getStyleClass().add("pestana");
        restablecer.setOnAction(e -> {
            posicion[0] = 0.5;
            posicion[1] = 0.5;
            zoom.setValue(1);
            actualizar.run();
        });

        VBox contenido = new VBox(12, vista, new Label("ZOOM"), zoom, restablecer);
        contenido.setAlignment(Pos.CENTER);
        contenido.setPadding(new Insets(12));

        ButtonType aplicar = new ButtonType("APLICAR", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> editor = new Dialog<>();
        editor.setTitle("ENCUADRAR PORTADA");
        editor.setHeaderText("ARRASTRA LA IMAGEN O AJUSTA EL ZOOM");
        editor.getDialogPane().getStylesheets().setAll(hojas);
        editor.getDialogPane().setContent(contenido);
        editor.getDialogPane().getButtonTypes().addAll(aplicar, cancelar);
        Terminal.dialogo(editor, "ENCUADRAR PORTADA", "boton-accion", "APLICAR");

        if (editor.showAndWait().filter(b -> b == aplicar).isEmpty()) return;

        Rectangle2D recorte = vista.getViewport();
        int x = (int) Math.floor(recorte.getMinX());
        int y = (int) Math.floor(recorte.getMinY());
        int ancho = Math.max(1, Math.min((int) fuente.getWidth() - x,
                (int) Math.floor(recorte.getWidth())));
        int alto = Math.max(1, Math.min((int) fuente.getHeight() - y,
                (int) Math.floor(recorte.getHeight())));
        try {
            WritableImage resultadoEncuadre = new WritableImage(
                    fuente.getPixelReader(), x, y, ancho, alto);
            pegarImagenDelPortapapeles(resultadoEncuadre);
            nombreImagen.setText("PORTADA ENCUADRADA");
        } catch (RuntimeException ex) {
            mostrarError(filaPortada, "No se pudo aplicar el encuadre: " + ex.getMessage());
        }
    }

    private HBox crearPortada() {
        StackPane marco = new StackPane(vistaPrevia);
        marco.getStyleClass().add("vista-previa");
        marco.setMinSize(112, 142);
        marco.setPrefSize(112, 142);
        marco.setMaxSize(112, 142);
        vistaPrevia.setFitWidth(104);
        vistaPrevia.setFitHeight(134);
        vistaPrevia.setPreserveRatio(true);

        Button elegir = new Button("ELEGIR...");
        elegir.getStyleClass().add("pestana");
        Button pegar = new Button("PEGAR");
        pegar.getStyleClass().add("pestana");
        pegar.setOnAction(e -> {
            Clipboard portapapeles = Clipboard.getSystemClipboard();
            try {
                if (portapapeles.hasImage()) {
                    pegarImagenDelPortapapeles(portapapeles.getImage());
                } else if (portapapeles.hasFiles()
                        && portapapeles.getFiles().size() == 1) {
                    usarArchivoPortada(portapapeles.getFiles().get(0));
                } else {
                    throw new IllegalArgumentException(
                            "Copia una imagen o un unico archivo de imagen.");
                }
            } catch (IllegalArgumentException ex) {
                mostrarError(filaPortada, ex.getMessage());
            }
        });
        Button encuadrar = new Button("ENCUADRAR");
        encuadrar.getStyleClass().add("pestana");
        encuadrar.setOnAction(e -> abrirEditorEncuadre());
        Button quitar = new Button("QUITAR");
        quitar.getStyleClass().add("pestana");
        nombreImagen.getStyleClass().add("subtitulo");
        if (original != null && original.getRutaImagen() != null) nombreImagen.setText("IMAGEN ACTUAL");

        elegir.setOnAction(e -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Elige una imagen");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File archivo = selector.showOpenDialog(cuerpo.getScene().getWindow());
            if (archivo == null) return;
            try {
                usarArchivoPortada(archivo);
            } catch (IllegalArgumentException ex) {
                mostrarError(filaPortada, ex.getMessage());
            }
        });
        quitar.setOnAction(e -> {
            limpiarTemporalPortada();
            archivoNuevo = null;
            quitarImagen = true;
            nombreImagen.setText("SIN IMAGEN");
            refrescarVistaPrevia();
            modificado = true;
        });

        marco.setOnDragOver(evento -> {
            Dragboard arrastre = evento.getDragboard();
            if (evento.getGestureSource() != marco
                    && arrastre.hasFiles()
                    && arrastre.getFiles().size() == 1) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });

        marco.setOnDragDropped(evento -> {
            Dragboard arrastre = evento.getDragboard();
            boolean aceptado = false;
            if (arrastre.hasFiles() && arrastre.getFiles().size() == 1) {
                try {
                    usarArchivoPortada(arrastre.getFiles().get(0));
                    aceptado = true;
                } catch (IllegalArgumentException ex) {
                    mostrarError(filaPortada, ex.getMessage());
                }
            }
            evento.setDropCompleted(aceptado);
            evento.consume();
        });

        VBox lado = new VBox(8, new HBox(8, elegir, pegar, quitar), encuadrar, nombreImagen);
        refrescarVistaPrevia();
        return new HBox(14, marco, lado);
    }

    private void refrescarVistaPrevia() {
        Image imagen = null;
        if (archivoNuevo != null) {
            Image candidata = new Image(archivoNuevo.toURI().toString(), 104, 134, true, true);
            if (!candidata.isError()) imagen = candidata;
        } else if (!quitarImagen && original != null) {
            imagen = GestorImagenes.imagenDe(original, 104, 134);
        }
        if (imagen == null) imagen = GestorImagenes.iconoDeTipo(nombreTipo() + "S", 104, 134);
        vistaPrevia.setImage(imagen);
    }

    private Estado estadoActual() {
        return (Estado) grupoEstado.getSelectedToggle().getUserData();
    }

    private void aplicarVisibilidad() {
        if (seccionProgreso == null) return;
        Estado estado = estadoActual();
        for (Fila f : filas) {
            boolean ver = f.visibleEn() == null || f.visibleEn().contains(estado);
            f.caja().setVisible(ver);
            f.caja().setManaged(ver);
            if (!ver) limpiar(f);
        }
        boolean conProgreso = CON_AVANCE.contains(estado);
        seccionProgreso.setVisible(conProgreso);
        seccionProgreso.setManaged(conProgreso);
    }

    private void actualizarNota() {
        List<Double> valores = criterios.values().stream().map(FilaCriterio::getValor).toList();
        Double calculada = ReglasForm.promedio(valores);
        double anterior = (original == null) ? 0 : original.getNota();

        notaViva.getChildren().clear();
        notaViva.setAlignment(Pos.CENTER_LEFT);
        if (calculada == null && anterior <= 0) {
            Label sin = new Label("SIN NOTA");
            sin.getStyleClass().add("subtitulo");
            notaViva.getChildren().add(sin);
            return;
        }
        double nota = (calculada != null) ? calculada : anterior;
        Label numero = new Label(Formato.decimal(nota));
        numero.getStyleClass().add("nota");
        notaViva.getChildren().addAll(numero, new EtiquetaTier(Tier.de(nota), null));
    }

    private void revisarFechas() {
        limpiarDe(inicio);
        limpiarDe(termino);
        LocalDate desde = inicio.valorProperty().get();
        LocalDate hasta = termino.valorProperty().get();
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            mostrarError(termino, "El termino no puede ser anterior al inicio.");
        }
    }

    private Fila buscarFila(Node nodo) {
        for (Node n = nodo; n != null; n = n.getParent()) {
            for (Fila f : filas) {
                if (f.campo() == n) return f;
            }
        }
        return null;
    }

    private void mostrarError(Node campo, String texto) {
        Fila fila = buscarFila(campo);
        if (fila == null) {
            mensaje.setText(texto);
            return;
        }
        fila.error().setText("!! " + texto);
        fila.campo().getStyleClass().remove("campo-error");
        fila.campo().getStyleClass().add("campo-error");
        fila.error().setVisible(true);
        fila.error().setManaged(true);
        desplazarHasta(fila);
    }

    private void mostrarErrorDelModelo(String texto) {
        List<Fila> visibles = filas.stream().filter(f -> f.caja().isManaged()).toList();
        int indice = ReglasForm.indiceDeCampo(texto, visibles.stream().map(Fila::etiqueta).toList());
        if (indice >= 0) {
            mostrarError(visibles.get(indice).campo(), texto);
        } else {
            mensaje.setText(texto);
        }
    }

    private void limpiar(Fila fila) {
        fila.error().setText("");
        fila.campo().getStyleClass().remove("campo-error");
        fila.error().setVisible(false);
        fila.error().setManaged(false);
    }

    private void limpiarDe(Node campo) {
        Fila fila = buscarFila(campo);
        if (fila != null) limpiar(fila);
    }

    private void limpiarErrores() {
        mensaje.setText("");
        filas.forEach(this::limpiar);
    }

    private void desplazarACriterios() {
        dialogo.getDialogPane().applyCss();
        dialogo.getDialogPane().layout();
        Bounds enEscena = seccionCriterios.localToScene(seccionCriterios.getBoundsInLocal());
        if (enEscena == null) return;
        double y = cuerpo.sceneToLocal(enEscena).getMinY();
        double recorrido = cuerpo.getHeight() - scroll.getViewportBounds().getHeight();
        if (recorrido > 0) scroll.setVvalue(Math.max(0, Math.min(1, (y - 8) / recorrido)));
    }

    private void desplazarHasta(Fila fila) {
        cuerpo.applyCss();
        cuerpo.layout();
        Bounds enEscena = fila.caja().localToScene(fila.caja().getBoundsInLocal());
        if (enEscena == null) return;
        double y = cuerpo.sceneToLocal(enEscena).getMinY();
        double recorrido = cuerpo.getHeight() - scroll.getViewportBounds().getHeight();
        if (recorrido > 0) scroll.setVvalue(Math.max(0, Math.min(1, y / recorrido)));
    }

    private boolean preguntar(String texto, String si, String no) {
        ButtonType botonSi = new ButtonType(si, ButtonBar.ButtonData.OK_DONE);
        ButtonType botonNo = new ButtonType(no, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, botonSi, botonNo);
        alerta.setTitle("CONFIRMAR");
        alerta.setHeaderText(null);
        alerta.initOwner(dialogo.getDialogPane().getScene().getWindow());
        alerta.getDialogPane().getStylesheets().setAll(hojas);

        Button b1 = (Button) alerta.getDialogPane().lookupButton(botonSi);
        Button b2 = (Button) alerta.getDialogPane().lookupButton(botonNo);
        b1.getStyleClass().add("boton-dialogo");
        b2.getStyleClass().addAll("boton-dialogo", "boton-accion");
        Terminal.corchetes(b1);
        Terminal.corchetes(b2);
        alerta.setHeaderText("CONFIRMAR");
        b1.setDefaultButton(false);
        b2.setDefaultButton(true);
        Terminal.dialogo(alerta, "CONFIRMAR", null, null);

        return alerta.showAndWait().filter(b -> b == botonSi).isPresent();
    }

    private boolean puedeDescartar() {
        if (descarteConfirmado) return true;

        if (!modificado) {
            try {
                demoraBorrador.stop();
                capturarBorrador = false;
                borrarBorradorSiNuevo();
            } catch (RuntimeException ex) {
                mensaje.setText("!! No se pudo borrar el borrador: " + ex.getMessage());
                return false;
            }
            return true;
        }

        if (preguntar("Hay cambios sin guardar. ¿Descartarlos?",
                "DESCARTAR", "SEGUIR EDITANDO")) {
            try {
                demoraBorrador.stop();
                capturarBorrador = false;
                borrarBorradorSiNuevo();
            } catch (RuntimeException ex) {
                mensaje.setText("!! No se pudo borrar el borrador: " + ex.getMessage());
                return false;
            }
            descarteConfirmado = true;
            return true;
        }
        return false;
    }

    private boolean tituloIgual(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private T construir(T existente) {
        String t = titulo.getText().trim();
        Estado e = estadoActual();
        boolean conAvance = CON_AVANCE.contains(e);
        boolean conTermino = CON_TERMINO.contains(e);

        double p = conAvance ? avance.getValor() : 0;
        if (e == Estado.TERMINADO && avanceAutomaticoAlTerminar()) p = 100;
        boolean sinIni = conAvance && sinInicio.isSelected();
        boolean sinFin = conTermino && sinTermino.isSelected();
        LocalDate ini = (conAvance && !sinIni) ? inicio.getValor() : null;
        LocalDate fin = (conTermino && !sinFin) ? termino.getValor() : null;
        if (ini != null && fin != null && fin.isBefore(ini)) {
            throw new ErrorCampo(termino, "El termino no puede ser anterior al inicio.");
        }

        int veces = conAvance ? entero(repeticiones, "Las veces consumidas") : 0;
        if (e == Estado.TERMINADO && veces < 1) veces = 1;
        String textoComentario = comentario.getText();

        Map<String, Double> notas = new LinkedHashMap<>();
        criterios.forEach((nombre, fila) -> notas.put(nombre, fila.getValor()));

        T prueba = crear(t, 0, e);
        aplicarComunes(prueba, t, e, p, ini, fin, sinIni, sinFin, notas, veces, textoComentario);
        if (existente == null && e == Estado.TERMINADO && veces == 1) {
            prueba.setFechaPrimeraFinalizacion(fin);
        }
        prueba.validarCoherencia();
        boolean tituloCambio = existente == null || !tituloIgual(existente.getTitulo(), t);
        if (comprobador != null && tituloCambio && comprobador.existe(prueba.getTipo(), t, existente)) {
            String texto = "Ya tienes «" + t + "» en " + prueba.getTipo() + ". ¿Guardar de todas formas?";
            if (!preguntar(texto, "GUARDAR IGUAL", "REVISAR")) return null;
        }

        String rutaAnterior = (existente == null) ? null : existente.getRutaImagen();
        String ruta = rutaAnterior;
        if (archivoNuevo != null) {
            ruta = GestorImagenes.copiar(archivoNuevo);
            portadaNuevaPendiente = ruta;
        } else if (quitarImagen) {
            ruta = null;
        }

        if (existente != null) {
            LocalDate primera = existente.getFechaPrimeraFinalizacion();
            if (e == Estado.TERMINADO && veces == 1
                    && existente.getVecesConsumido() <= 1) primera = fin;
            prueba.setFechaPrimeraFinalizacion(primera);
            prueba.setNumero(existente.getNumero());
            prueba.setFavorito(existente.isFavorito());
        }
        prueba.setRutaImagen(ruta);
        return prueba;
    }

    private void aplicarComunes(T item, String t, Estado e, double p,
                                LocalDate ini, LocalDate fin, boolean sinIni, boolean sinFin,
                                Map<String, Double> notas,
                                int veces, String textoComentario) {
        item.setTitulo(t);
        item.setEstado(e);
        item.setPorcentaje(p);
        item.setFechaTermino(null);
        item.setFechaInicio(ini);
        item.setFechaTermino(fin);
        item.setSinFechaInicio(sinIni);
        item.setSinFechaTermino(sinFin);
        item.setVecesConsumido(veces);
        item.setComentario(textoComentario);
        item.setConQuien(conQuien.getText());
        notas.forEach(item::setCriterio);
    }

    private void cargar(T item) {
        titulo.setText(item.getTitulo());
        chips.get(item.getEstado()).setSelected(true);
        avance.setValor(item.getPorcentaje());
            inicio.setValor(item.getFechaInicio());
            termino.setValor(item.getFechaTermino());
            sinInicio.setSelected(item.isSinFechaInicio());
            sinTermino.setSelected(item.isSinFechaTermino());
        repeticiones.setText(String.valueOf(item.getVecesConsumido()));
        comentario.setText(item.getComentario());
        conQuien.setText(item.getConQuien());
        item.getCriterios().forEach((nombre, valor) -> {
            FilaCriterio fila = criterios.get(nombre);
            if (fila != null) fila.setValor(valor);
        });
        cargarPropios(item);
    }

    protected String texto(TextField f) {
        return f.getText().trim();
    }

    protected int entero(TextField f, String campo) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException ex) {
            throw new ErrorCampo(f, campo + " debe ser un numero entero.");
        }
    }

    protected double decimal(TextField f, String campo) {
        try {
            return Double.parseDouble(f.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new ErrorCampo(f, campo + " debe ser un numero.");
        }
    }
}