package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReglasTerminoTest {
    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);

    private Libro libroTerminadoEl(LocalDate fecha) {
        Libro libro = new Libro("The Witcher", 8.5, Estado.PENDIENTE,
                "Andrzej Sapkowski", "Fantasia", 320, 1993);
        Transicion.TERMINAR.aplicar(libro, fecha);
        return libro;
    }

    @Test void hoyAyerYHaceVariosDias() {
        assertEquals("TERMINADO HOY", ReglasTermino.textoDias(libroTerminadoEl(HOY), HOY));
        assertEquals("TERMINADO AYER", ReglasTermino.textoDias(libroTerminadoEl(HOY.minusDays(1)), HOY));
        assertEquals("TERMINADO HACE 5 DIAS", ReglasTermino.textoDias(libroTerminadoEl(HOY.minusDays(5)), HOY));
    }

    @Test void sinTerminarOSinDatosNoDevuelveTexto() {
        Libro pendiente = new Libro("X", 0, Estado.PENDIENTE, "A", "G", 100, 2000);
        assertNull(ReglasTermino.textoDias(pendiente, HOY));
        assertNull(ReglasTermino.textoDias(null, HOY));
        assertNull(ReglasTermino.textoDias(libroTerminadoEl(HOY), null));
    }
}