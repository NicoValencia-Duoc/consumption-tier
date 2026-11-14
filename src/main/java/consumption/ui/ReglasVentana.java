package consumption.ui;

import java.util.List;

public final class ReglasVentana {

    public static final double ALTO_TITULO = 40;
    public static final double MIN_ANCHO_VISIBLE = 120;
    public static final double MIN_ALTO_VISIBLE = 30;

    private ReglasVentana() {
    }

    public static boolean tituloVisible(double x, double y, double ancho, List<double[]> pantallas) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(ancho)) return false;
        for (double[] p : pantallas) {
            double visibleAncho = Math.min(x + ancho, p[0] + p[2]) - Math.max(x, p[0]);
            double visibleAlto = Math.min(y + ALTO_TITULO, p[1] + p[3]) - Math.max(y, p[1]);
            if (visibleAncho >= MIN_ANCHO_VISIBLE && visibleAlto >= MIN_ALTO_VISIBLE) return true;
        }
        return false;
    }
}
