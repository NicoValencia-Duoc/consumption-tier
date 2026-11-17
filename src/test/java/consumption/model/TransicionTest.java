package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransicionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);

    private Libro libro(Estado estado) {
        return new Libro("The Witcher", 8.5, estado,
                "Andrzej Sapkowski", "Fantasia", 320, 1993);
    }

    @Test
    void empezarDesdePendiente() {
        Libro item = libro(Estado.PENDIENTE);
        Transicion.EMPEZAR.aplicar(item, HOY);
        assertEquals(Estado.EN_PROGRESO, item.getEstado());
        assertEquals(HOY, item.getFechaInicio());
        assertEquals(0, item.getVecesConsumido());
    }

    @Test
    void terminarPrimeraVezCuentaUno() {
        Libro item = libro(Estado.PENDIENTE);
        Transicion.TERMINAR.aplicar(item, HOY);
        assertEquals(Estado.TERMINADO, item.getEstado());
        assertEquals(HOY, item.getFechaTermino());
        assertEquals(100, item.getPorcentaje());
        assertEquals(1, item.getVecesConsumido());
    }

    @Test
    void repetirNoIncrementaHastaVolverATerminar() {
        Libro item = libro(Estado.PENDIENTE);
        Transicion.TERMINAR.aplicar(item, HOY);
        Transicion.REPETIR.aplicar(item, HOY.plusDays(1));

        assertEquals(Estado.EN_PROGRESO, item.getEstado());
        assertEquals(0, item.getPorcentaje());
        assertNull(item.getFechaInicio());
        assertNull(item.getFechaTermino());
        assertEquals(1, item.getVecesConsumido());

        Transicion.TERMINAR.aplicar(item, HOY.plusDays(2));
        assertEquals(Estado.TERMINADO, item.getEstado());
        assertEquals(2, item.getVecesConsumido());
    }

    @Test
    void abandonarNoSumaConsumo() {
        Libro item = libro(Estado.PENDIENTE);
        Transicion.EMPEZAR.aplicar(item, HOY);
        Transicion.ABANDONAR.aplicar(item, HOY.plusDays(1));
        assertEquals(Estado.ABANDONADO, item.getEstado());
        assertEquals(0, item.getVecesConsumido());
    }

    @Test
    void repetirNoEstaPermitidoDesdePendiente() {
        Libro item = libro(Estado.PENDIENTE);
        assertThrows(IllegalArgumentException.class,
                () -> Transicion.REPETIR.aplicar(item, HOY));
        assertEquals(Estado.PENDIENTE, item.getEstado());
    }

    @Test
    void noPermiteSuperarMaximoDeConsumiciones() {
        Libro item = libro(Estado.EN_PROGRESO);
        item.setVecesConsumido(ItemConsumption.MAX_VECES);
        assertThrows(IllegalArgumentException.class,
                () -> Transicion.TERMINAR.aplicar(item, HOY));
        assertEquals(Estado.EN_PROGRESO, item.getEstado());
        assertEquals(ItemConsumption.MAX_VECES, item.getVecesConsumido());
    }

    @Test
    void noPermiteTerminarConInicioPosterior() {
        Libro item = libro(Estado.EN_PROGRESO);
        item.setFechaInicio(HOY.plusDays(1));
        assertThrows(IllegalArgumentException.class,
                () -> Transicion.TERMINAR.aplicar(item, HOY));
        assertEquals(Estado.EN_PROGRESO, item.getEstado());
    }

    @Test
    void validaTituloNotaYComentario() {
        Libro item = libro(Estado.PENDIENTE);
        assertThrows(IllegalArgumentException.class, () -> item.setTitulo(" "));
        assertThrows(IllegalArgumentException.class, () -> item.setNota(10.1));
        assertThrows(IllegalArgumentException.class,
                () -> item.setComentario("x".repeat(ItemConsumption.MAX_COMENTARIO + 1)));
    }

    @Test
    void pendienteNoPuedeTenerAvance() {
        Libro item = libro(Estado.PENDIENTE);
        item.setPorcentaje(20);
        assertThrows(IllegalArgumentException.class, item::validarCoherencia);
    }

    @Test
    void terminadoRequiereFechaOSinRegistro() {
        Libro item = libro(Estado.TERMINADO);
        assertThrows(IllegalArgumentException.class, item::validarCoherencia);
        item.setSinFechaTermino(true);
        assertDoesNotThrow(item::validarCoherencia);
    }
}
