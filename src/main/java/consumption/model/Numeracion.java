package consumption.model;

import java.util.List;

public final class Numeracion {

    private Numeracion() {
    }

    public static int siguiente(List<? extends ItemConsumption> items) {
        int maximo = 0;
        for (ItemConsumption i : items) maximo = Math.max(maximo, i.getNumero());
        return maximo + 1;
    }

    public static boolean asignar(List<? extends ItemConsumption> items) {
        int maximo = siguiente(items) - 1;
        boolean cambio = false;
        for (ItemConsumption i : items) {
            if (i.getNumero() <= 0) {
                i.setNumero(++maximo);
                cambio = true;
            }
        }
        return cambio;
    }

    public static String etiqueta(int numero) {
        return numero > 0 ? String.format("#%03d", numero) : "#---";
    }
}