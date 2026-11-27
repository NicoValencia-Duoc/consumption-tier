package consumption.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

final class BarraContinua extends Region {
    private static final double SEPARACION_VALOR = 3;
    private static final double[] GUIAS_PORCENTAJE = {0.25, 0.50, 0.75, 1.0};

    private final boolean vertical;
    private final double fraccion;
    private final double altoGrafico;
    private final double reservaValor;
    private final double margenInferior;
    private final Region valor;
    private final Region pista;
    private final Region relleno;
    private final List<Region> guias = new ArrayList<>();

    static BarraContinua horizontal(double fraccion, double alto) {
        return new BarraContinua(false, fraccion, alto, 0, 0, null, null, false);
    }

    static BarraContinua vertical(double fraccion, double alto, double reservaValor, double margenInferior,
                                  Region valor, String claseColor, boolean conGuias) {
        return new BarraContinua(true, fraccion, alto, reservaValor, margenInferior, valor, claseColor, conGuias);
    }

    private BarraContinua(boolean vertical, double fraccion, double altoGrafico,
                          double reservaValor, double margenInferior, Region valor, String claseColor, boolean conGuias) {
        this.vertical = vertical;
        this.fraccion = Double.isFinite(fraccion) ? Math.max(0, Math.min(1, fraccion)) : 0;
        this.altoGrafico = altoGrafico;
        this.reservaValor = reservaValor;
        this.margenInferior = margenInferior;
        this.valor = valor;
        setSnapToPixel(false);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(vertical ? Double.MAX_VALUE : altoGrafico);

        pista = vertical ? null : region("grafico-pista");
        if (pista != null) getChildren().add(pista);
        if (conGuias) {
            for (double ignorada : GUIAS_PORCENTAJE) {
                Region guia = region("grafico-guia-continua");
                guias.add(guia);
                getChildren().add(guia);
            }
        }
        relleno = region("grafico-relleno");
        relleno.getStyleClass().add(claseColor == null ? "grafico-relleno-normal" : claseColor);
        relleno.setVisible(this.fraccion > 0);
        getChildren().add(relleno);
        if (valor != null) {
            getChildren().add(valor);
        }
    }

    private static Region region(String clase) {
        Region region = new Region();
        region.getStyleClass().add(clase);
        region.setManaged(false);
        region.setMouseTransparent(true);
        region.setSnapToPixel(false);
        return region;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return valor == null ? 0 : valor.prefWidth(-1);
    }

    @Override
    protected double computePrefWidth(double alto) {
        return Math.max(computeMinWidth(alto), vertical ? 24 : 120);
    }

    @Override
    protected double computeMinHeight(double ancho) {
        if (!vertical) return altoGrafico;
        double altoValor = valor == null ? 0 : valor.prefHeight(-1) + SEPARACION_VALOR;
        return altoGrafico + Math.max(reservaValor, altoValor) + margenInferior;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        return computeMinHeight(ancho);
    }

    @Override
    protected void layoutChildren() {
        double ancho = Math.max(0, getWidth());
        double alto = Math.max(0, getHeight());
        if (!vertical) {
            pista.resizeRelocate(0, 0, ancho, alto);
            relleno.resizeRelocate(0, 0, ancho * fraccion, alto);
            return;
        }

        double base = Math.max(0, alto - margenInferior);
        double altoPlot = Math.min(altoGrafico, base);
        double inicioPlot = base - altoPlot;
        double altoRelleno = altoPlot * fraccion;
        double inicioRelleno = base - altoRelleno;
        for (int i = 0; i < guias.size(); i++) {
            double y = base - altoPlot * GUIAS_PORCENTAJE[i];
            guias.get(i).resizeRelocate(0, Math.max(inicioPlot, y), ancho, Math.min(1, altoPlot));
        }
        relleno.resizeRelocate(0, inicioRelleno, ancho, altoRelleno);
        if (valor != null) {
            double w = Math.min(ancho, valor.prefWidth(-1));
            double h = valor.prefHeight(w);
            double y = Math.max(0, inicioRelleno - h - SEPARACION_VALOR);
            valor.resizeRelocate((ancho - w) / 2, y, w, h);
        }
    }

    static final class Eje extends Region {
        private static final double[] FRACCIONES = {1, .75, .5, .25, 0};
        private static final String[] TEXTOS = {"100%", "75%", "50%", "25%", "0%"};
        private final List<Label> etiquetas = new ArrayList<>();
        private final double altoGrafico;
        private final double reservaValor;
        private final double margenInferior;

        Eje(double altoGrafico, double reservaValor, double margenInferior) {
            this.altoGrafico = altoGrafico;
            this.reservaValor = reservaValor;
            this.margenInferior = margenInferior;
            setSnapToPixel(false);
            setMaxHeight(Double.MAX_VALUE);
            for (String texto : TEXTOS) {
                Label etiqueta = new Label(texto);
                etiqueta.getStyleClass().add("grafico-eje");
                etiquetas.add(etiqueta);
                getChildren().add(etiqueta);
            }
        }

        @Override
        protected double computePrefWidth(double alto) {
            return etiquetas.stream().mapToDouble(e -> e.prefWidth(-1)).max().orElse(0);
        }

        @Override
        protected double computeMinWidth(double alto) {
            return computePrefWidth(alto);
        }

        @Override
        protected double computeMinHeight(double ancho) {
            double mediaEtiqueta = etiquetas.stream().mapToDouble(e -> e.prefHeight(-1) / 2).max().orElse(0);
            return altoGrafico + Math.max(reservaValor, mediaEtiqueta) + margenInferior;
        }

        @Override
        protected double computePrefHeight(double ancho) {
            return computeMinHeight(ancho);
        }

        @Override
        protected void layoutChildren() {
            double alto = getHeight();
            double base = Math.max(0, alto - margenInferior);
            double altoPlot = Math.min(altoGrafico, base);
            for (int i = 0; i < etiquetas.size(); i++) {
                Label e = etiquetas.get(i);
                double w = e.prefWidth(-1);
                double h = e.prefHeight(-1);
                double y = base - altoPlot * FRACCIONES[i] - h / 2;
                e.resizeRelocate(getWidth() - w, Math.max(0, Math.min(alto - h, y)), w, h);
            }
        }
    }
}
