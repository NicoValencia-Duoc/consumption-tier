package consumption.ui;

final class ReglasEstadisticas {
    static final double ANCHO_MINIMO_TARJETA = 270;
    static final double ANCHO_MINIMO_MINI = 330;
    static final double ANCHO_DOS_SUPERIORES = 960;
    static final double SEPARACION = 14;

    record Modo(boolean dosSuperiores, int columnasMini, int columnasTarjetas) { }

    private ReglasEstadisticas() { }

    static double factorEscala(int escalaTexto) {
        return Math.max(90, Math.min(115, escalaTexto)) / 100.0;
    }

    static int columnas(double ancho, double anchoMinimo, double separacion, int maximo) {
        int n = (int) Math.floor((ancho + separacion) / (anchoMinimo + separacion));
        return Math.max(1, Math.min(maximo, n));
    }

    static int segmentosPorFraccion(double fraccion, int total) {
        if (fraccion <= 0 || total <= 0) return 0;
        int llenos = (int) Math.round(fraccion * total);
        return Math.max(1, Math.min(total, llenos));
    }

    static Modo modo(double ancho, int escalaTexto) {
        double factor = factorEscala(escalaTexto);
        boolean dos = ancho >= ANCHO_DOS_SUPERIORES * factor;
        double anchoMini = dos ? ancho * 0.58 : ancho;
        return new Modo(dos,
                columnas(anchoMini, ANCHO_MINIMO_MINI * factor, SEPARACION, 2),
                columnas(ancho, ANCHO_MINIMO_TARJETA * factor, SEPARACION, 4));
    }
}