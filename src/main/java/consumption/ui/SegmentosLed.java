package consumption.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

final class SegmentosLed extends Region {
    private static final double SEPARACION = 2;

    private final boolean vertical;
    private final int total;
    private final int[] posicionGuias;
    private final List<Region> segmentos = new ArrayList<>();
    private final List<Region> guias = new ArrayList<>();

    SegmentosLed(boolean vertical, int total, int llenos, String claseLleno, int... posicionGuias) {
        this.vertical = vertical;
        this.total = total;
        this.posicionGuias = posicionGuias;
        for (int ignorada : posicionGuias) {
            Region guia = new Region();
            guia.getStyleClass().add("led-guia");
            guia.setMouseTransparent(true);
            guias.add(guia);
            getChildren().add(guia);
        }
        for (int i = 0; i < total; i++) {
            Region segmento = new Region();
            segmento.getStyleClass().add("led-e");
            if (i < llenos) segmento.getStyleClass().add(claseLleno != null ? claseLleno : "led-e-on");
            segmentos.add(segmento);
            getChildren().add(segmento);
        }
    }

    @Override
    protected double computePrefWidth(double alto) {
        return vertical ? 40 : 120;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        return vertical ? 120 : 12;
    }

    @Override
    protected void layoutChildren() {
        if (total <= 0) return;
        double ancho = getWidth();
        double alto = getHeight();
        if (vertical) {
            double celda = Math.max(1, (alto - SEPARACION * (total - 1)) / total);
            for (int i = 0; i < total; i++) {
                double y = alto - (i + 1) * celda - i * SEPARACION;
                segmentos.get(i).resizeRelocate(0, y, ancho, celda);
            }
            for (int g = 0; g < guias.size(); g++) {
                int k = posicionGuias[g];
                double y = alto - k * celda - (k - 1) * SEPARACION - SEPARACION / 2;
                guias.get(g).resizeRelocate(0, Math.max(0, y), ancho, 1);
            }
        } else {
            double celda = Math.max(1, (ancho - SEPARACION * (total - 1)) / total);
            for (int i = 0; i < total; i++) {
                segmentos.get(i).resizeRelocate(i * (celda + SEPARACION), 0, celda, alto);
            }
        }
    }

    static final class Eje extends Region {
        private final List<Label> etiquetas = new ArrayList<>();
        private final double[] fracciones;

        Eje(double[] fracciones, String[] textos) {
            this.fracciones = fracciones;
            for (String texto : textos) {
                Label etiqueta = new Label(texto);
                etiqueta.getStyleClass().add("grafico-eje");
                etiquetas.add(etiqueta);
                getChildren().add(etiqueta);
            }
        }

        @Override
        protected double computePrefWidth(double alto) {
            double mayor = 0;
            for (Label etiqueta : etiquetas) mayor = Math.max(mayor, etiqueta.prefWidth(-1));
            return mayor;
        }

        @Override
        protected double computeMinWidth(double alto) {
            return computePrefWidth(alto);
        }

        @Override
        protected void layoutChildren() {
            double alto = getHeight();
            for (int i = 0; i < etiquetas.size(); i++) {
                Label etiqueta = etiquetas.get(i);
                double w = etiqueta.prefWidth(-1);
                double h = etiqueta.prefHeight(-1);
                double y = Math.max(0, Math.min(alto - h, alto * (1 - fracciones[i]) - h / 2));
                etiqueta.resizeRelocate(getWidth() - w, y, w, h);
            }
        }
    }
}