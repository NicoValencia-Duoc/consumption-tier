package consumption.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

public class FilaCriterio extends HBox {

    private final Slider deslizador = new Slider(0, 10, 5);
    private final Label texto = new Label("N/A");
    private final ObjectProperty<Double> valor = new SimpleObjectProperty<>();
    private boolean interno;

    public FilaCriterio() {
        super(10);
        setAlignment(Pos.CENTER_LEFT);

        deslizador.setPrefWidth(220);
        deslizador.setBlockIncrement(0.5);
        texto.setMinWidth(42);
        texto.getStyleClass().add("valor-criterio");

        Button limpiar = new Button("N/A");
        limpiar.getStyleClass().add("pestana");
        limpiar.setOnAction(e -> setValor(null));

        deslizador.valueProperty().addListener((obs, antes, ahora) -> {
            if (interno) return;
            fijar(redondear(ahora.doubleValue()));
        });

        deslizador.setOnMouseReleased(e -> {
            if (valor.get() == null) fijar(redondear(deslizador.getValue()));
        });
        valor.addListener((obs, antes, ahora) -> refrescar());

        getChildren().addAll(deslizador, texto, limpiar);
        refrescar();
    }

    private static double redondear(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private void fijar(double v) {
        interno = true;
        deslizador.setValue(v);
        interno = false;
        valor.set(v);
    }

    public void setValor(Double v) {
        if (v != null) {
            interno = true;
            deslizador.setValue(v);
            interno = false;
        }
        valor.set(v);
    }

    public Double getValor() {
        return valor.get();
    }

    public ObjectProperty<Double> valorProperty() {
        return valor;
    }

    private void refrescar() {
        Double v = valor.get();
        texto.setText(v == null ? "N/A" : String.format("%.1f", v));
        deslizador.setOpacity(v == null ? 0.4 : 1);
    }
}