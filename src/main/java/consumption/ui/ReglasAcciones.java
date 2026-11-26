package consumption.ui;

import consumption.model.Transicion;

import java.util.List;

public final class ReglasAcciones {

    public static final int MAX_TITULOS = 3;
    public static final int SEGUNDOS_DESHACER = 8;
    private static final int LARGO_TITULO = 50;

    private ReglasAcciones() {
    }

    public static String textoEliminado(int cantidad) {
        return cantidad == 1 ? "1 REGISTRO ELIMINADO" : cantidad + " REGISTROS ELIMINADOS";
    }

    public static String textoRestaurado(int cantidad) {
        return cantidad == 1 ? "REGISTRO RESTAURADO" : cantidad + " REGISTROS RESTAURADOS";
    }

    public static String textoConfirmarEliminar(List<String> titulos) {
        int n = titulos.size();
        String pie = "\n\nTendras " + SEGUNDOS_DESHACER + " segundos para deshacerlo.";
        if (n == 1) {
            return "¿Eliminar «" + corto(titulos.get(0)) + "»?" + pie;
        }
        StringBuilder texto = new StringBuilder("¿Eliminar " + n + " registros?\n");
        for (int i = 0; i < Math.min(n, MAX_TITULOS); i++) {
            texto.append("\n• ").append(corto(titulos.get(i)));
        }
        if (n > MAX_TITULOS) {
            texto.append("\n• ... y ").append(n - MAX_TITULOS).append(" mas");
        }
        return texto.append(pie).toString();
    }

    public static String textoAviso(Transicion transicion) {
        return switch (transicion) {
            case EMPEZAR -> "EMPEZADO";
            case ABANDONAR -> "ABANDONADO";
            case TERMINAR -> "TERMINADO";
            case REPETIR -> "REPETIDO";
        };
    }

    public static double posicionDesplazamiento(double y, double alto, double alturaVista, double alturaContenido) {
        double recorrido = alturaContenido - alturaVista;
        if (recorrido <= 0) return 0;
        double deseado = y - (alturaVista - alto) / 2.0;
        return Math.max(0, Math.min(1, deseado / recorrido));
    }

    private static String corto(String titulo) {
        return titulo.length() <= LARGO_TITULO ? titulo : titulo.substring(0, LARGO_TITULO - 3) + "...";
    }
}
