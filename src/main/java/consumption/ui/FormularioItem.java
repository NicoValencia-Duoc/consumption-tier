package consumption.ui;

import consumption.data.GestorImagenes;
import consumption.model.Estado;
import consumption.model.ItemConsumption;
import consumption.model.Tier;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

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
    private T resultado;
    private T original;

    private final TextField titulo = new TextField();
    private final ToggleGroup grupoEstado = new ToggleGroup();
    private final Map<Estado, ToggleButton> chips = new EnumMap<>(Estado.class);
    private final TextField porcentaje = new TextField("0");
    private final BarraAvance barraAvance = new BarraAvance();
    private final CampoFecha inicio = new CampoFecha("El inicio");
    private final CampoFecha termino = new CampoFecha("El termino");
    private final Map<String, FilaCriterio> criterios = new LinkedHashMap<>();
    private final HBox notaViva = new HBox(10);

    private File archivoNuevo;
    private boolean quitarImagen;
    private final Label nombreImagen = new Label("SIN IMAGEN");
    private final ImageView vistaPrevia = new ImageView();
    private HBox filaPortada;

    protected abstract String nombreTipo();
    protected abstract List<String> nombresCriterios();
    protected abstract void agregarCamposPropios();
    protected abstract void cargarPropios(T item);
    protected abstract T crear(String titulo, double nota, Estado estado);
    protected abstract void aplicarPropios(T item);

    public Optional<T> mostrar(T existente, List<String> hojas) {
        this.original = existente;

        nuevaSeccion("IDENTIDAD");
        agregarFila("TITULO", titulo);
        agregarFila("ESTADO", crearChipsEstado());

        seccionProgreso = nuevaSeccion("PROGRESO");
        agregarFilaCondicional("AVANCE (%)", crearAvance(), CON_AVANCE);
        agregarFilaCondicional("INICIO", inicio, CON_AVANCE);
        agregarFilaCondicional("TERMINO", termino, CON_TERMINO);
        termino.setMinimo(() -> inicio.valorProperty().get());
        inicio.valorProperty().addListener((obs, antes, ahora) -> revisarFechas());
        termino.valorProperty().addListener((obs, antes, ahora) -> revisarFechas());

        nuevaSeccion("DATOS DE " + nombreTipo());
        agregarCamposPropios();

        nuevaSeccion("PORTADA");
        filaPortada = crearPortada();
        agregarFila("IMAGEN", filaPortada);

        nuevaSeccion("CRITERIOS · 0 A 10 · N/A SI NO APLICA");
        for (String criterio : nombresCriterios()) {
            FilaCriterio fila = new FilaCriterio();
            fila.valorProperty().addListener((obs, antes, ahora) -> actualizarNota());
            criterios.put(criterio, fila);
            agregarFila(criterio, fila);
        }
        agregarFila("NOTA FINAL", notaViva);

        if (existente != null) cargar(existente);
        aplicarVisibilidad();
        actualizarNota();

        cuerpo.setPadding(new Insets(14));
        cuerpo.getStyleClass().add("cuerpo-form");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(560);
        scroll.setPrefViewportHeight(480);

        mensaje.getStyleClass().add("mensaje-error");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(540);
        VBox raiz = new VBox(8, scroll, mensaje);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle((existente == null ? "AGREGAR " : "EDITAR ") + nombreTipo());
        dialogo.setHeaderText(existente == null ? TITULO_NUEVO : TITULO_EDICION);
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        dialogo.getDialogPane().setContent(raiz);
        ButtonType guardar = new ButtonType("GUARDAR", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, cancelar);

        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        Button botonCancelar = (Button) dialogo.getDialogPane().lookupButton(cancelar);
        botonGuardar.getStyleClass().addAll("boton-dialogo", "boton-accion");
        botonCancelar.getStyleClass().add("boton-dialogo");

        botonGuardar.addEventFilter(ActionEvent.ACTION, ev -> {
            limpiarErrores();
            try {
                resultado = construir(existente);
            } catch (ErrorCampo ex) {
                mostrarError(ex.getCampo(), ex.getMessage());
                ev.consume();
            } catch (IllegalArgumentException ex) {
                mostrarErrorDelModelo(ex.getMessage());
                ev.consume();
            }
        });

        Platform.runLater(titulo::requestFocus);
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        return respuesta.filter(b -> b == guardar).map(b -> resultado);
    }

    private VBox nuevaSeccion(String nombre) {
        Label cabecera = new Label(nombre);
        cabecera.getStyleClass().add("seccion-form");
        cabecera.setMaxWidth(Double.MAX_VALUE);
        Region linea = new Region();
        linea.getStyleClass().add("seccion-linea");

        VBox seccion = new VBox(8, cabecera, linea);
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
            texto.textProperty().addListener((obs, antes, ahora) -> limpiar(fila));
        }
    }

    private Node crearChipsEstado() {
        HBox caja = new HBox(6);
        for (Estado e : Estado.values()) {
            ToggleButton chip = new ToggleButton(e.getEtiqueta());
            chip.setToggleGroup(grupoEstado);
            chip.setUserData(e);
            chip.getStyleClass().addAll("chip", e.getClaseCss().replace("estado-", "chip-"));
            chips.put(e, chip);
            caja.getChildren().add(chip);
        }
        grupoEstado.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                aplicarVisibilidad();
            }
        });
        chips.get(Estado.PENDIENTE).setSelected(true);
        return caja;
    }

    private Node crearAvance() {
        porcentaje.setPrefColumnCount(5);
        porcentaje.textProperty().addListener((obs, antes, ahora) -> {
            barraAvance.setPorcentaje(leerNumero(ahora));
            limpiarDe(porcentaje);
        });
        HBox caja = new HBox(12, porcentaje, barraAvance);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
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
                GestorImagenes.validar(archivo);
            } catch (IllegalArgumentException ex) {
                mostrarError(filaPortada, ex.getMessage());
                return;
            }
            archivoNuevo = archivo;
            quitarImagen = false;
            nombreImagen.setText(archivo.getName());
            refrescarVistaPrevia();
        });
        quitar.setOnAction(e -> {
            archivoNuevo = null;
            quitarImagen = true;
            nombreImagen.setText("SIN IMAGEN");
            refrescarVistaPrevia();
        });

        VBox lado = new VBox(8, new HBox(8, elegir, quitar), nombreImagen);
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
        Label numero = new Label(String.format("%.1f", nota));
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
        fila.error().setText(texto);
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

    private void desplazarHasta(Fila fila) {
        cuerpo.applyCss();
        cuerpo.layout();
        Bounds enEscena = fila.caja().localToScene(fila.caja().getBoundsInLocal());
        if (enEscena == null) return;
        double y = cuerpo.sceneToLocal(enEscena).getMinY();
        double recorrido = cuerpo.getHeight() - scroll.getViewportBounds().getHeight();
        if (recorrido > 0) scroll.setVvalue(Math.max(0, Math.min(1, y / recorrido)));
    }

    private T construir(T existente) {
        String t = titulo.getText().trim();
        Estado e = estadoActual();
        boolean conAvance = CON_AVANCE.contains(e);
        boolean conTermino = CON_TERMINO.contains(e);

        double p = conAvance ? decimal(porcentaje, "El avance") : 0;
        LocalDate ini = conAvance ? inicio.getValor() : null;
        LocalDate fin = conTermino ? termino.getValor() : null;
        if (ini != null && fin != null && fin.isBefore(ini)) {
            throw new ErrorCampo(termino, "El termino no puede ser anterior al inicio.");
        }

        Map<String, Double> notas = new LinkedHashMap<>();
        criterios.forEach((nombre, fila) -> notas.put(nombre, fila.getValor()));

        T prueba = crear(t, 0, e);
        aplicarComunes(prueba, t, e, p, ini, fin, notas);
        prueba.validarCoherencia();

        String rutaAnterior = (existente == null) ? null : existente.getRutaImagen();
        String ruta = rutaAnterior;
        if (archivoNuevo != null) {
            ruta = GestorImagenes.copiar(archivoNuevo);
        } else if (quitarImagen) {
            ruta = null;
        }

        T item = prueba;
        if (existente != null) {
            aplicarComunes(existente, t, e, p, ini, fin, notas);
            aplicarPropios(existente);
            item = existente;
        }
        item.setRutaImagen(ruta);
        if (!Objects.equals(ruta, rutaAnterior)) GestorImagenes.eliminar(rutaAnterior);
        return item;
    }

    private void aplicarComunes(T item, String t, Estado e, double p,
                                LocalDate ini, LocalDate fin, Map<String, Double> notas) {
        item.setTitulo(t);
        item.setEstado(e);
        item.setPorcentaje(p);
        item.setFechaTermino(null);
        item.setFechaInicio(ini);
        item.setFechaTermino(fin);
        notas.forEach(item::setCriterio);
    }

    private void cargar(T item) {
        titulo.setText(item.getTitulo());
        chips.get(item.getEstado()).setSelected(true);
        porcentaje.setText(ReglasForm.numero(item.getPorcentaje()));
        inicio.setValor(item.getFechaInicio());
        termino.setValor(item.getFechaTermino());
        item.getCriterios().forEach((nombre, valor) -> {
            FilaCriterio fila = criterios.get(nombre);
            if (fila != null) fila.setValor(valor);
        });
        cargarPropios(item);
    }

    private static double leerNumero(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return 0;
        }
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