package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class FormularioItem<T extends ItemConsumption> {

    private final GridPane grilla = new GridPane();
    private final Label mensaje = new Label();
    private int fila = 0;
    private T resultado;

    private final TextField titulo = new TextField();
    private final ComboBox<Estado> estado = new ComboBox<>();
    private final TextField porcentaje = new TextField();
    private final DatePicker inicio = new DatePicker();
    private final DatePicker termino = new DatePicker();
    private final Map<String, TextField> campoCriterio = new LinkedHashMap<>();

    protected abstract String nombreTipo();
    protected abstract List<String> nombresCriterios();
    protected abstract void agregarCamposPropios();
    protected abstract void cargarPropios(T item);
    protected abstract T crear(String titulo, double nota, Estado estado);
    protected abstract void aplicarPropios(T item);

    public Optional<T> mostrar(T existente, List<String> hojas) {
        estado.getItems().setAll(Estado.values());
        porcentaje.setText("0");
        estado.setValue(Estado.PENDIENTE);

        agregarFila("TITULO", titulo);
        agregarFila("ESTADO", estado);
        agregarFila("AVANCE (%)", porcentaje);
        agregarFila("INICIO", inicio);
        agregarFila("TERMINO", termino);
        agregarCamposPropios();

        Label seccion = new Label("CRITERIOS DE NOTA - 0 A 10 - VACIO = N/A");
        seccion.getStyleClass().add("subtitulo");
        grilla.add(seccion, 0, fila++, 2, 1);
        for (String criterio : nombresCriterios()) {
            TextField campo = new TextField();
            campo.setPromptText("N/A");
            campoCriterio.put(criterio, campo);
            agregarFila(criterio, campo);
        }
        Label aviso = new Label("La nota final se calcula sola con el promedio de los criterios que completes.");
        aviso.getStyleClass().add("subtitulo");
        aviso.setWrapText(true);
        aviso.setMaxWidth(320);
        grilla.add(aviso, 0, fila++, 2, 1);

        if (existente != null) cargar(existente);

        mensaje.getStyleClass().add("mensaje-error");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(320);
        grilla.add(mensaje, 0, fila, 2, 1);
        grilla.setHgap(10);
        grilla.setVgap(8);
        grilla.setPadding(new Insets(16));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle((existente == null ? "AGREGAR " : "EDITAR ") + nombreTipo());
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        dialogo.getDialogPane().setContent(grilla);
        ButtonType guardar = new ButtonType("GUARDAR", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                resultado = construir(existente);
            } catch (IllegalArgumentException ex) {
                mensaje.setText(ex.getMessage());
                ev.consume();
            }
        });

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        return respuesta.filter(b -> b == guardar).map(b -> resultado);
    }

    private T construir(T existente) {
        String t = titulo.getText().trim();
        Estado e = estado.getValue();
        double p = decimal(porcentaje, "El avance");
        LocalDate ini = inicio.getValue();
        LocalDate fin = termino.getValue();

        Map<String, Double> notas = new LinkedHashMap<>();
        for (Map.Entry<String, TextField> c : campoCriterio.entrySet()) {
            String valor = c.getValue().getText().trim();
            notas.put(c.getKey(), valor.isEmpty() ? null : decimal(c.getValue(), c.getKey()));
        }

        T prueba = crear(t, 0, e);
        aplicarComunes(prueba, t, e, p, ini, fin, notas);
        prueba.validarCoherencia();
        if (existente == null) return prueba;

        aplicarComunes(existente, t, e, p, ini, fin, notas);
        aplicarPropios(existente);
        return existente;
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
        estado.setValue(item.getEstado());
        porcentaje.setText(String.valueOf(item.getPorcentaje()));
        inicio.setValue(item.getFechaInicio());
        termino.setValue(item.getFechaTermino());
        item.getCriterios().forEach((nombre, valor) -> {
            TextField campo = campoCriterio.get(nombre);
            if (campo != null) campo.setText(String.valueOf(valor));
        });
        cargarPropios(item);
    }

    protected void agregarFila(String etiqueta, Node campo) {
        grilla.add(new Label(etiqueta), 0, fila);
        grilla.add(campo, 1, fila);
        fila++;
    }

    protected String texto(TextField f) {
        return f.getText().trim();
    }

    protected int entero(TextField f, String campo) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(campo + " debe ser un numero entero.");
        }
    }

    protected double decimal(TextField f, String campo) {
        try {
            return Double.parseDouble(f.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(campo + " debe ser un numero.");
        }
    }
}