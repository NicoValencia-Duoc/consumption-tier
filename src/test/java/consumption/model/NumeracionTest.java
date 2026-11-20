package consumption.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NumeracionTest {
    private Juego juego(String titulo) {
        return new Juego(titulo, 0, Estado.PENDIENTE, "PC", "OTRO", "AUTOR", 2020, 0);
    }

    @Test void asignaNumerosSinCambiarLosExistentes() {
        Juego primero = juego("Primero");
        Juego segundo = juego("Segundo");
        segundo.setNumero(7);
        assertTrue(Numeracion.asignar(List.of(primero, segundo)));
        assertEquals(8, primero.getNumero());
        assertEquals(7, segundo.getNumero());
    }

    @Test void segundaAsignacionNoHaceCambios() {
        Juego item = juego("Ejemplo");
        assertTrue(Numeracion.asignar(List.of(item)));
        assertFalse(Numeracion.asignar(List.of(item)));
        assertEquals(2, Numeracion.siguiente(List.of(item)));
    }

    @Test void etiquetaConservaFormato() {
        assertEquals("#007", Numeracion.etiqueta(7));
        assertEquals("#---", Numeracion.etiqueta(0));
    }
}
