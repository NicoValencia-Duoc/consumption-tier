package consumption.ui;

import consumption.data.RepositorioJson;
import consumption.model.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.*;

final class CampoGeneros extends VBox {
    private final RepositorioJson repositorio;
    private final List<ItemConsumption> items;
    private final AmbitoGenero ambito;
    private final Runnable alCambiar, alCambiarBiblioteca;
    private final LinkedHashMap<UUID, ReferenciaGenero> seleccion = new LinkedHashMap<>();
    private final FlowPane chips = new FlowPane(5, 5);
    private final TextField entrada = new TextField();
    private final VBox lista = new VBox(4);
    private final Label mensaje = new Label();
    private boolean mostrarSinUso;
    private UUID confirmar;

    CampoGeneros(RepositorioJson repositorio, List<ItemConsumption> items, AmbitoGenero ambito,
                 List<ReferenciaGenero> iniciales, Runnable alCambiar, Runnable alCambiarBiblioteca) {
        super(6);
        this.repositorio = repositorio; this.items = items; this.ambito = ambito;
        this.alCambiar = alCambiar; this.alCambiarBiblioteca = alCambiarBiblioteca;
        setMinWidth(0); setMaxWidth(Double.MAX_VALUE); getStyleClass().add("campo-generos");
        chips.setMinWidth(0);
        entrada.setPromptText("ESCRIBIR GENERO..."); entrada.setMinWidth(0);
        entrada.textProperty().addListener((o,a,b) -> { confirmar = null; refrescar(); });
        entrada.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER || (ev.getCode() == KeyCode.TAB && !ev.isShiftDown()
                    && !entrada.getText().isBlank() && !coincidencias().isEmpty())) {
                ev.consume(); agregarEntrada(ev.getCode() == KeyCode.ENTER);
            }
        });
        Label titulo = new Label("TAGS EN " + ambito.etiqueta() + ":");
        titulo.getStyleClass().add("subtitulo"); titulo.setWrapText(true);
        ScrollPane scroll = new ScrollPane(lista); scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.setPrefViewportHeight(140);
        scroll.setMinHeight(75); scroll.setMaxHeight(200); scroll.setMinWidth(0);
        mensaje.getStyleClass().add("mensaje-error"); mensaje.setWrapText(true);
        mensaje.managedProperty().bind(mensaje.textProperty().isNotEmpty()); mensaje.visibleProperty().bind(mensaje.managedProperty());
        VBox campo = new VBox(4, chips, entrada); campo.getStyleClass().add("generos-chips");
        campo.setMinWidth(0); getChildren().addAll(campo, titulo, scroll, mensaje);
        cargar(iniciales);
    }

    List<ReferenciaGenero> referencias() {
        return seleccion.values().stream().filter(r -> repositorio.bibliotecaGeneros().buscar(r.id())
                .filter(t -> t.activo() && t.ambito() == ambito && t.nombre().equals(r.nombre())).isPresent()).toList();
    }
    void cargar(List<ReferenciaGenero> refs) {
        repositorio.bibliotecaGeneros().resolver(ambito, refs);
        seleccion.clear(); refs.forEach(r -> seleccion.put(r.id(), r)); refrescar();
    }
    private List<TagGenero> coincidencias() {
        String clave = NombreGenero.clave(entrada.getText());
        return repositorio.bibliotecaGeneros().activos(ambito).stream()
                .filter(t -> clave.isEmpty() || t.clave().contains(clave))
                .sorted(Comparator.comparing((TagGenero t) -> !t.clave().equals(clave))
                        .thenComparing(t -> !t.clave().startsWith(clave)).thenComparing(TagGenero::clave)).toList();
    }
    private void agregarEntrada(boolean permitirCrear) {
        String texto = entrada.getText().strip();
        if (NombreGenero.clave(texto).isEmpty()) return;
        try {
            var existentes = coincidencias();
            TagGenero tag;
            if (!existentes.isEmpty()) tag = existentes.getFirst();
            else if (permitirCrear) { tag = repositorio.altaGenero(ambito, texto, items); notificarBiblioteca(); }
            else return;
            agregar(tag);
        } catch (RuntimeException ex) { mensaje.setText("!! " + ex.getMessage()); }
    }
    private void agregar(TagGenero tag) {
        boolean nuevo = seleccion.putIfAbsent(tag.id(), ReferenciaGenero.de(tag)) == null;
        entrada.clear(); mensaje.setText(""); confirmar = null; refrescar();
        if (nuevo) alCambiar.run(); entrada.requestFocus();
    }
    private void notificarBiblioteca() {
        try { alCambiarBiblioteca.run(); }
        catch (RuntimeException ex) { System.err.println("Biblioteca guardada; fallo de refresco: " + ex.getMessage()); }
    }
    private Button boton(String texto, boolean peligro) {
        Button b = Terminal.corchetes(new Button(texto));
        b.getStyleClass().addAll("pestana", "a2-button", "genero-boton");
        if (peligro) b.getStyleClass().add("boton-peligro");
        b.setDefaultButton(false); b.setCancelButton(false);
        return b;
    }
    private void refrescar() {
        chips.getChildren().clear();
        for (var ref : referencias()) {
            Label nombre = new Label(NombreGenero.mostrar(ref.nombre()));
            nombre.setWrapText(true); nombre.setMinWidth(0);
            nombre.maxWidthProperty().bind(javafx.beans.binding.Bindings.max(widthProperty().subtract(90), 40));
            Button quitar = boton("X", false);
            quitar.setAccessibleText("QUITAR " + NombreGenero.mostrar(ref.nombre()) + " DE ESTE ITEM");
            quitar.getStyleClass().add("genero-seleccionado");
            quitar.setOnAction(e -> { seleccion.remove(ref.id()); confirmar = null; refrescar(); alCambiar.run(); });
            HBox chip = new HBox(4, nombre, quitar); chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            chip.getStyleClass().add("genero-chip"); chips.getChildren().add(chip);
        }
        lista.getChildren().clear();
        List<TagGenero> sinUso = new ArrayList<>();
        var biblioteca = repositorio.bibliotecaGeneros();
        var usos = biblioteca.usos(ambito, items);
        var cero = new BibliotecaGeneros.Uso(0, 0, 0);
        for (var tag : coincidencias()) {
            var uso = usos.getOrDefault(tag.id(), cero);
            if (uso.total() == 0) sinUso.add(tag); else lista.getChildren().add(fila(tag, uso));
        }
        if (!sinUso.isEmpty()) {
            boolean filtrando = !entrada.getText().isBlank();
            Button plegar = boton("TAGS SIN USO (" + sinUso.size() + ") " + (mostrarSinUso || filtrando ? "-" : "+"), false);
            plegar.setMaxWidth(Double.MAX_VALUE); plegar.setOnAction(e -> { mostrarSinUso = !mostrarSinUso; refrescar(); });
            lista.getChildren().add(plegar);
            if (mostrarSinUso || filtrando) for (var tag : sinUso) lista.getChildren().add(fila(tag, cero));
        }
    }
    private VBox fila(TagGenero tag, BibliotecaGeneros.Uso uso) {
        Button agregar = boton(tag.etiqueta(), false); agregar.setWrapText(true); agregar.setMinWidth(0);
        agregar.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(agregar, Priority.ALWAYS);
        if (!entrada.getText().isBlank()) agregar.getStyleClass().add("genero-coincidente");
        agregar.setOnAction(e -> agregar(tag));
        Label cantidad = new Label(Integer.toString(uso.total())); cantidad.getStyleClass().add("subtitulo");
        Button borrar = boton("X", true);
        borrar.setAccessibleText("BORRAR " + tag.etiqueta() + " DE LA BIBLIOTECA");
        borrar.setOnAction(e -> { confirmar = tag.id(); refrescar(); });
        HBox linea = new HBox(5, agregar, cantidad, borrar); linea.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox fila = new VBox(4, linea); fila.setMinWidth(0);
        if (tag.id().equals(confirmar)) {
            String desglose = ambito == AmbitoGenero.AUDIOVISUAL ? " · " + uso.peliculas() + " PELICULAS · " + uso.series() + " SERIES" : "";
            Label pregunta = new Label("BORRAR «" + tag.etiqueta() + "» DE " + uso.total() + " ITEMS?" + desglose
                    + "\nINMEDIATO. CANCELAR EL FORMULARIO NO LO REVIERTE.");
            pregunta.setWrapText(true); pregunta.getStyleClass().add("genero-alerta");
            Button si = boton("SI", true), no = boton("NO", false);
            si.setOnAction(e -> {
                try {
                    if (!repositorio.bibliotecaGeneros().uso(ambito, tag.id(), items).equals(uso)) { refrescar(); return; }
                    repositorio.bajaGenero(ambito, tag.id(), items);
                    seleccion.remove(tag.id()); confirmar = null; mensaje.setText("");
                    refrescar(); alCambiar.run(); notificarBiblioteca();
                } catch (RuntimeException ex) { mensaje.setText("!! " + ex.getMessage()); }
            });
            no.setOnAction(e -> { confirmar = null; refrescar(); });
            fila.getChildren().addAll(pregunta, new HBox(5, si, no));
        }
        return fila;
    }
}
