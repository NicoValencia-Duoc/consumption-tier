package consumption.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglasArranqueTest {

    @Test
    void noSePuedeOmitirAlPrincipio() {
        assertFalse(ReglasArranque.omitible(0));
        assertFalse(ReglasArranque.omitible(999));
    }

    @Test
    void sePuedeOmitirDesdeElLimite() {
        assertTrue(ReglasArranque.omitible(ReglasArranque.OMITIBLE_DESDE_MS));
        assertTrue(ReglasArranque.omitible(4000));
    }
}
