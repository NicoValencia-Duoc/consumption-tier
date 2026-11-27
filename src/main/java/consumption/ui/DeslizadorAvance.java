package consumption.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DeslizadorAvance extends VBox {

    private static final double TOPE_BASE = 100;
    private static final double TOPE_EXTENDIDO = 200;
    private static final double ANCHO = 200;

    private final Slider deslizador = new Slider(0, TOPE_BASE, 0);
    private final BarraAvance barra = new BarraAvance();
    private final Button boton = new Button("[ SUPERAR 100% ]");
    { boton.getStyleClass().add("a2-button"); }
    private final DoubleProperty valor = new SimpleDoubleProperty(0);
    private final boolean permiteExceso;

    private boolean extendido;
    private boolean interno;
    private boolean cambioDelUsuario;
    private Runnable alConfirmar;

    public DeslizadorAvance(boolean permiteExceso) {
        super(6);
        this.permiteExceso = permiteExceso;

        deslizador.setPrefWidth(ANCHO);
        deslizador.setMinWidth(ANCHO);
        deslizador.setBlockIncrement(1);

        boton.getStyleClass().addAll("pestana", "boton-superar");
        boton.setVisible(permiteExceso);
        boton.setManaged(permiteExceso);
        boton.setOnAction(e -> alternarExtendido());

        deslizador.valueProperty().addListener((obs, antes, ahora) -> {
            if (interno) return;
            cambioDelUsuario = true;
            fijar(Math.round(ahora.doubleValue()));
        });

        deslizador.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> confirmar());
        deslizador.addEventHandler(KeyEvent.KEY_RELEASED, e -> confirmar());
        valor.addListener((obs, antes, ahora) -> refrescar());

        HBox linea = new HBox(10, deslizador, boton);
        linea.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(linea, barra);
        refrescar();
    }

    public DoubleProperty valorProperty() {
        return valor;
    }

    public double getValor() {
        return valor.get();
    }

    public void setValor(double v) {
        if (permiteExceso && v > TOPE_BASE) activarExtendido(true);
        interno = true;
        deslizador.setValue(v);
        interno = false;
        valor.set(v);
    }

    public void setAlConfirmar(Runnable accion) {
        this.alConfirmar = accion;
    }

    private void fijar(double v) {
        interno = true;
        deslizador.setValue(v);
        interno = false;
        valor.set(v);
    }

    private void confirmar() {
        if (!cambioDelUsuario) return;
        cambioDelUsuario = false;
        if (alConfirmar != null) alConfirmar.run();
    }

    private void alternarExtendido() {
        if (!extendido) {
            activarExtendido(true);
            return;
        }
        if (valor.get() > TOPE_BASE && !confirmarRecorte()) return;
        activarExtendido(false);
        if (valor.get() > TOPE_BASE) {
            cambioDelUsuario = true;
            fijar(TOPE_BASE);
            confirmar();
        }
    }

    private void activarExtendido(boolean activo) {
        extendido = activo;
        interno = true;
        deslizador.setMax(activo ? TOPE_EXTENDIDO : TOPE_BASE);
        interno = false;
        boton.setText(activo ? "[ VOLVER A 100% ]" : "[ SUPERAR 100% ]");
        barra.setExtendida(activo);
        marcar(deslizador, "extendido", activo);
        refrescar();
    }

    private void refrescar() {
        double v = valor.get();
        barra.setPorcentaje(v);
        marcar(deslizador, "sobre-cien", v > TOPE_BASE);
    }

    private static void marcar(Node nodo, String clase, boolean activa) {
        nodo.getStyleClass().remove(clase);
        if (activa) nodo.getStyleClass().add(clase);
    }

    private boolean confirmarRecorte() {
        ButtonType recortar = new ButtonType("RECORTAR", ButtonBar.ButtonData.OK_DONE);
        ButtonType mantener = new ButtonType("MANTENER", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "El avance es " + String.format("%.0f", valor.get()) + "%. Volver a 100% lo recortaria a 100%. ¿Continuar?",
                recortar, mantener);
        alerta.setTitle("CONFIRMAR");
        alerta.setHeaderText("CONFIRMAR");
        if (getScene() != null) {
            alerta.initOwner(getScene().getWindow());
            alerta.getDialogPane().getStylesheets().setAll(getScene().getStylesheets());
        }
        Button b1 = (Button) alerta.getDialogPane().lookupButton(recortar);
        b1.getStyleClass().add("a2-button");
        Button b2 = (Button) alerta.getDialogPane().lookupButton(mantener);
        b2.getStyleClass().add("a2-button");
        b1.getStyleClass().add("boton-dialogo");
        b2.getStyleClass().addAll("boton-dialogo", "boton-accion");
        b1.setDefaultButton(false);
        b2.setDefaultButton(true);
        Terminal.corchetes(b1);
        Terminal.corchetes(b2);
        return alerta.showAndWait().filter(b -> b == recortar).isPresent();
    }
}
