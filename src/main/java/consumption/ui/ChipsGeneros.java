package consumption.ui;

import consumption.model.NombreGenero;
import consumption.model.ReferenciaGenero;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

final class ChipsGeneros extends Pane {
    private static final double GAP = 4;
    private final List<Label> chips = new ArrayList<>();
    private final Label restantes = etiqueta("");

    static Label etiqueta(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("genero-chip-lectura");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    static FlowPane detalle(List<ReferenciaGenero> refs) {
        FlowPane panel = new FlowPane(GAP, GAP);
        panel.setMinWidth(0);
        panel.setPrefWrapLength(300);
        for (ReferenciaGenero ref : refs) {
            Label label = etiqueta(NombreGenero.mostrar(ref.nombre()));
            label.setWrapText(true);
            label.setMinWidth(0);
            label.maxWidthProperty().bind(panel.widthProperty());
            panel.getChildren().add(label);
        }
        return panel;
    }

    ChipsGeneros(List<ReferenciaGenero> refs) {
        for (ReferenciaGenero ref : refs) chips.add(etiqueta(NombreGenero.mostrar(ref.nombre())));
        getChildren().addAll(chips);
        getChildren().add(restantes);
        setMouseTransparent(true);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
    }

    @Override protected double computeMinWidth(double h) { return 0; }
    @Override protected double computePrefWidth(double h) {
        return chips.stream().mapToDouble(l -> l.prefWidth(-1) + GAP).sum();
    }
    @Override protected double computePrefHeight(double w) {
        return chips.stream().mapToDouble(l -> l.prefHeight(-1)).max().orElse(0);
    }
    @Override protected void layoutChildren() {
        double disponible = getWidth();
        double usado = 0;
        int caben = 0;
        double todos = chips.stream().mapToDouble(c -> Math.ceil(c.prefWidth(-1))).sum()
                + Math.max(0, chips.size() - 1) * GAP;
        if (todos <= disponible) caben = chips.size();
        for (Label chip : chips) {
            if (caben == chips.size()) break;
            double ancho = Math.ceil(chip.prefWidth(-1));
            int faltan = chips.size() - caben - 1;
            double reserva = faltan > 0 ? GAP + anchoIndicador(faltan) : 0;
            double nuevo = usado + (caben > 0 ? GAP : 0) + ancho;
            if (nuevo + reserva > disponible) break;
            usado = nuevo;
            caben++;
        }
        double x = 0;
        for (int i = 0; i < chips.size(); i++) {
            Label chip = chips.get(i);
            chip.setVisible(i < caben);
            if (i < caben) {
                double w = Math.ceil(chip.prefWidth(-1));
                colocar(chip, x, w);
                x += w + GAP;
            }
        }
        restantes.setVisible(caben < chips.size());
        if (caben < chips.size()) {
            restantes.setText("+" + (chips.size() - caben));
            colocar(restantes, x, Math.max(0, Math.min(disponible - x, restantes.prefWidth(-1))));
        }
    }
    private double anchoIndicador(int n) {
        javafx.scene.text.Text medida = new javafx.scene.text.Text("+" + n);
        medida.setFont(restantes.getFont());
        return Math.ceil(medida.getLayoutBounds().getWidth() + restantes.getInsets().getLeft() + restantes.getInsets().getRight());
    }
    private void colocar(Label label, double x, double ancho) {
        double alto = Math.min(getHeight(), label.prefHeight(ancho));
        label.resizeRelocate(x, Math.max(0, (getHeight() - alto) / 2), ancho, alto);
    }
}
