package consumption.ui;

import java.util.List;

final class ClasificadorSonidos {
    enum Tipo { NINGUNO, CLIC, PELIGRO, MENU, FILA }

    record Rasgos(boolean deshabilitado, boolean control, boolean encabezadoColumna, boolean peligro,
                  boolean itemMenu, boolean celda, boolean celdaVacia, boolean tarjeta, boolean manito) { }

    private ClasificadorSonidos() { }

    static Tipo clasificar(List<Rasgos> cadena) {
        for (Rasgos nodo : cadena) {
            if (nodo.deshabilitado()) return Tipo.NINGUNO;
            if (nodo.control() || nodo.encabezadoColumna()) {
                return nodo.peligro() ? Tipo.PELIGRO : Tipo.CLIC;
            }
            if (nodo.itemMenu()) return Tipo.MENU;
            if (nodo.celda()) return nodo.celdaVacia() ? Tipo.NINGUNO : Tipo.FILA;
            if (nodo.tarjeta() || nodo.manito()) return Tipo.FILA;
        }
        return Tipo.NINGUNO;
    }
}
