package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NivelMawTest {

    private static Libro libro(Estado estado) {
        return new Libro("Titulo", 8.0, estado, "Autor", "Fantasia", 300, 2000);
    }

    @Test
    void tramosPorCantidad() {
        assertEquals(NivelMaw.SIN_NIVEL, NivelMaw.porCantidad(0));
        assertEquals(NivelMaw.HAMBRIENTA, NivelMaw.porCantidad(1));
        assertEquals(NivelMaw.HAMBRIENTA, NivelMaw.porCantidad(24));
        assertEquals(NivelMaw.DESPIERTA, NivelMaw.porCantidad(25));
        assertEquals(NivelMaw.DESPIERTA, NivelMaw.porCantidad(49));
        assertEquals(NivelMaw.VORAZ, NivelMaw.porCantidad(50));
        assertEquals(NivelMaw.VORAZ, NivelMaw.porCantidad(74));
        assertEquals(NivelMaw.INSACIABLE, NivelMaw.porCantidad(75));
        assertEquals(NivelMaw.INSACIABLE, NivelMaw.porCantidad(99));
        assertEquals(NivelMaw.DEIDAD, NivelMaw.porCantidad(100));
        assertEquals(NivelMaw.DEIDAD, NivelMaw.porCantidad(5000));
    }

    @Test
    void cantidadNegativaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> NivelMaw.porCantidad(-1));
    }

    @Test
    void progresoDentroDeCadaNivel() {
        assertEquals(0, NivelMaw.SIN_NIVEL.progreso(0));
        assertEquals(4, NivelMaw.HAMBRIENTA.progreso(1));
        assertEquals(52, NivelMaw.DESPIERTA.progreso(37));
        assertEquals(4, NivelMaw.VORAZ.progreso(50));
        assertEquals(100, NivelMaw.VORAZ.progreso(74));
        assertEquals(100, NivelMaw.DEIDAD.progreso(100));
        assertEquals(100, NivelMaw.DEIDAD.progreso(250));
    }

    @Test
    void etiquetaDeSinNivelLlevaEspacio() {
        assertEquals("SIN NIVEL", NivelMaw.SIN_NIVEL.etiqueta());
        assertEquals("DEIDAD", NivelMaw.DEIDAD.etiqueta());
    }

    @Test
    void cuentaLosTerminadosAlgunaVezAunqueEstenRepitiendose() {
        Libro terminado = libro(Estado.PENDIENTE);
        Transicion.TERMINAR.aplicar(terminado, LocalDate.of(2026, 11, 1));
        Libro repitiendo = libro(Estado.PENDIENTE);
        Transicion.TERMINAR.aplicar(repitiendo, LocalDate.of(2026, 11, 1));
        Transicion.REPETIR.aplicar(repitiendo, LocalDate.of(2026, 11, 2));
        Libro pendiente = libro(Estado.PENDIENTE);
        Libro enProgreso = libro(Estado.PENDIENTE);
        Transicion.EMPEZAR.aplicar(enProgreso, LocalDate.of(2026, 11, 2));
        Libro abandonado = libro(Estado.PENDIENTE);
        Transicion.EMPEZAR.aplicar(abandonado, LocalDate.of(2026, 11, 2));
        Transicion.ABANDONAR.aplicar(abandonado, LocalDate.of(2026, 11, 3));

        List<ItemConsumption> items = List.of(terminado, repitiendo, pendiente, enProgreso, abandonado);
        assertEquals(2, NivelMaw.contarTerminados(items));
    }
}