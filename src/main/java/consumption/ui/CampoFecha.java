package consumption.ui;

import consumption.ui.FormularioItem;
import consumption.ui.ReglasForm;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Supplier;

public class CampoFecha extends HBox {

    private static final String[] DIAS = {"L", "M", "M", "J", "V", "S", "D"};

    private final String nombre;
    private final TextField campo = new TextField();
    private final Popup popup = new Popup();
    private final VBox panel = new VBox(8);
    private final ObjectProperty<LocalDate> valor = new SimpleObjectProperty<>();
    private Supplier<LocalDate> minimo = () -> null;
    private YearMonth mes = YearMonth.now();

    public CampoFecha(String nombre) {
        super(6);
        this.nombre = nombre;
        setAlignment(Pos.CENTER_LEFT);

        campo.setPromptText("dd/mm/aaaa");
        campo.setPrefColumnCount(9);
        campo.setTextFormatter(new TextFormatter<String>(
                cambio -> cambio.getControlNewText().matches("[0-9/]{0,10}") ? cambio : null));
        campo.textProperty().addListener((obs, antes, ahora) -> valor.set(leerONull(ahora)));

        Button abrir = new Button("CAL ▾");
        abrir.getStyleClass().add("pestana");
        abrir.setOnAction(e -> {
            if (popup.isShowing()) popup.hide();
            else abrirCalendario();
        });

        panel.getStyleClass().add("cal-panel");
        popup.getContent().add(panel);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        getChildren().addAll(campo, abrir);
    }

    public void setMinimo(Supplier<LocalDate> minimo) {
        this.minimo = minimo;
    }

    public ObjectProperty<LocalDate> valorProperty() {
        return valor;
    }

    public void setValor(LocalDate fecha) {
        campo.setText(ReglasForm.formatoFecha(fecha));
    }

    public LocalDate getValor() {
        try {
            return ReglasForm.leerFecha(campo.getText());
        } catch (IllegalArgumentException ex) {
            throw new FormularioItem.ErrorCampo(this, nombre + " debe ser una fecha valida con formato dd/mm/aaaa.");
        }
    }

    private static LocalDate leerONull(String texto) {
        try {
            return ReglasForm.leerFecha(texto);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void abrirCalendario() {
        LocalDate base = valor.get() != null ? valor.get() : LocalDate.now();
        mes = YearMonth.from(base);
        dibujar();

        panel.getStylesheets().setAll(getScene().getRoot().getStylesheets());
        Bounds caja = campo.localToScreen(campo.getBoundsInLocal());
        popup.show(campo, caja.getMinX(), caja.getMaxY() + 2);
    }

    private void dibujar() {
        panel.getChildren().clear();

        Label titulo = new Label(ReglasForm.nombreMes(mes.getMonth()) + " " + mes.getYear());
        titulo.getStyleClass().add("cal-titulo");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setAlignment(Pos.CENTER);
        HBox.setHgrow(titulo, Priority.ALWAYS);
        HBox cabecera = new HBox(4, navegar("«", -12), navegar("‹", -1), titulo, navegar("›", 1), navegar("»", 12));
        cabecera.setAlignment(Pos.CENTER);

        GridPane rejilla = new GridPane();
        rejilla.setHgap(2);
        rejilla.setVgap(2);
        for (int c = 0; c < 7; c++) {
            Label dia = new Label(DIAS[c]);
            dia.getStyleClass().add("cal-dia-nombre");
            dia.setMinWidth(32);
            dia.setAlignment(Pos.CENTER);
            rejilla.add(dia, c, 0);
        }

        LocalDate hoy = LocalDate.now();
        LocalDate elegida = valor.get();
        LocalDate desde = minimo.get();
        int vacias = ReglasForm.desfase(mes);
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            LocalDate fecha = mes.atDay(d);
            int posicion = vacias + d - 1;
            Button boton = new Button(String.valueOf(d));
            boton.getStyleClass().add("cal-dia");
            if (fecha.equals(hoy)) boton.getStyleClass().add("cal-hoy");
            if (fecha.equals(elegida)) boton.getStyleClass().add("cal-sel");
            if (desde != null && fecha.isBefore(desde)) boton.setDisable(true);
            boton.setOnAction(e -> elegir(fecha));
            rejilla.add(boton, posicion % 7, 1 + posicion / 7);
        }

        Button botonHoy = new Button("HOY");
        botonHoy.getStyleClass().addAll("pestana", "boton-accion");
        botonHoy.setDisable(desde != null && hoy.isBefore(desde));
        botonHoy.setOnAction(e -> elegir(hoy));
        Button limpiar = new Button("LIMPIAR");
        limpiar.getStyleClass().add("pestana");
        limpiar.setOnAction(e -> elegir(null));
        HBox pie = new HBox(8, botonHoy, limpiar);
        pie.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(cabecera, rejilla, pie);
        panel.setPadding(new Insets(10));
    }

    private Button navegar(String texto, int meses) {
        Button b = new Button(texto);
        b.getStyleClass().add("cal-nav");
        b.setOnAction(e -> {
            mes = mes.plusMonths(meses);
            dibujar();
        });
        return b;
    }

    private void elegir(LocalDate fecha) {
        setValor(fecha);
        popup.hide();
    }
}
