package consumption.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static consumption.ui.ClasificadorSonidos.Rasgos;
import static consumption.ui.ClasificadorSonidos.Tipo;
import static org.junit.jupiter.api.Assertions.*;

class ClasificadorSonidosTest {

    private static Rasgos nodo() {
        return new Rasgos(false, false, false, false, false, false, false, false, false);
    }

    private static Rasgos con(boolean deshabilitado, boolean control, boolean encabezado, boolean peligro,
                              boolean itemMenu, boolean celda, boolean vacia, boolean tarjeta, boolean manito) {
        return new Rasgos(deshabilitado, control, encabezado, peligro, itemMenu, celda, vacia, tarjeta, manito);
    }

    @Test
    void sinNadaClicableNoSuena() {
        assertEquals(Tipo.NINGUNO, ClasificadorSonidos.clasificar(List.of(nodo(), nodo())));
        assertEquals(Tipo.NINGUNO, ClasificadorSonidos.clasificar(List.of()));
    }

    @Test
    void botonHaceClic() {
        Rasgos boton = con(false, true, false, false, false, false, false, false, false);
        assertEquals(Tipo.CLIC, ClasificadorSonidos.clasificar(List.of(nodo(), boton, nodo())));
    }

    @Test
    void botonPeligroSuenaPeligro() {
        Rasgos boton = con(false, true, false, true, false, false, false, false, false);
        assertEquals(Tipo.PELIGRO, ClasificadorSonidos.clasificar(List.of(boton)));
    }

    @Test
    void encabezadoDeColumnaHaceClic() {
        Rasgos encabezado = con(false, false, true, false, false, false, false, false, false);
        assertEquals(Tipo.CLIC, ClasificadorSonidos.clasificar(List.of(nodo(), encabezado)));
    }

    @Test
    void itemDeMenuHaceTic() {
        Rasgos item = con(false, false, false, false, true, false, false, false, false);
        assertEquals(Tipo.MENU, ClasificadorSonidos.clasificar(List.of(nodo(), item)));
    }

    @Test
    void filaConDatosSuenaYFilaVaciaNo() {
        Rasgos llena = con(false, false, false, false, false, true, false, false, false);
        Rasgos vacia = con(false, false, false, false, false, true, true, false, false);
        assertEquals(Tipo.FILA, ClasificadorSonidos.clasificar(List.of(nodo(), llena)));
        assertEquals(Tipo.NINGUNO, ClasificadorSonidos.clasificar(List.of(nodo(), vacia, con(false, true, false, false, false, false, false, false, false))));
    }

    @Test
    void tarjetaOManitoSuenaFila() {
        Rasgos tarjeta = con(false, false, false, false, false, false, false, true, false);
        Rasgos manito = con(false, false, false, false, false, false, false, false, true);
        assertEquals(Tipo.FILA, ClasificadorSonidos.clasificar(List.of(nodo(), tarjeta)));
        assertEquals(Tipo.FILA, ClasificadorSonidos.clasificar(List.of(manito)));
    }

    @Test
    void deshabilitadoCortaAntesDeLosPadres() {
        Rasgos apagado = con(true, false, false, false, false, false, false, false, false);
        Rasgos boton = con(false, true, false, false, false, false, false, false, false);
        assertEquals(Tipo.NINGUNO, ClasificadorSonidos.clasificar(List.of(apagado, boton)));
    }

    @Test
    void gananElMasCercano() {
        Rasgos tarjeta = con(false, false, false, false, false, false, false, true, false);
        Rasgos boton = con(false, true, false, false, false, false, false, false, false);
        assertEquals(Tipo.CLIC, ClasificadorSonidos.clasificar(List.of(boton, tarjeta)));
        assertEquals(Tipo.FILA, ClasificadorSonidos.clasificar(List.of(tarjeta, boton)));
    }
}