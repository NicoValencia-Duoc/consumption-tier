package consumption.ui;

public final class ReglasTabla {

    public static final double TOPE = 200;

    private ReglasTabla() {
    }

    public static double escala(double porcentaje) {
        return porcentaje > 100 ? 200 : 100;
    }

    public static double fraccionNormal(double porcentaje) {
        double p = limitar(porcentaje);
        return Math.min(p, 100) / escala(p);
    }

    public static double fraccionExceso(double porcentaje) {
        double p = limitar(porcentaje);
        return p > 100 ? (p - 100) / 200 : 0;
    }

    private static double limitar(double p) {
        if (Double.isNaN(p) || p < 0) return 0;
        return Math.min(p, TOPE);
    }

    public static String textoRegistros(String tipo, int cantidad) {
        return tipo + " · " + cantidad + (cantidad == 1 ? " REGISTRO" : " REGISTROS");
    }

    public static String textoSeleccionados(int cantidad) {
        return cantidad + (cantidad == 1 ? " SELECCIONADO" : " SELECCIONADOS");
    }

    public static String textoEliminar(int cantidad) {
        return cantidad > 1 ? "ELIMINAR (" + cantidad + ")" : "ELIMINAR";
    }

    public static String textoSeleccionarTodo(boolean todasMarcadas) {
        return todasMarcadas ? "QUITAR SELECCION" : "SELECCIONAR TODO";
    }

    public static boolean debeAlternarFavorito(boolean esFavorito, boolean todasFavoritas) {
        return esFavorito == todasFavoritas;
    }
}
