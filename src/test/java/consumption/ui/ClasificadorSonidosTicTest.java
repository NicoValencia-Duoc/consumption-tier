package consumption.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClasificadorSonidosTicTest {

    @Test
    void elTicSuenaSoloEnBotonesHabilitados() {
        assertTrue(ClasificadorSonidos.suenaAlPasar(true, false));
        assertFalse(ClasificadorSonidos.suenaAlPasar(true, true));
        assertFalse(ClasificadorSonidos.suenaAlPasar(false, false));
        assertFalse(ClasificadorSonidos.suenaAlPasar(false, true));
    }

    @Test
    void elIntervaloMinimoEvitaQueSuenePorBarrerElMouse() {
        assertFalse(ClasificadorSonidos.intervaloCumplido(1_000_000L, 960_000L, 60_000L + 1));
        assertTrue(ClasificadorSonidos.intervaloCumplido(1_000_000L, 940_000L, 60_000L));
        assertTrue(ClasificadorSonidos.intervaloCumplido(5_000_000_000L, 0L, 60_000_000L));
    }
}