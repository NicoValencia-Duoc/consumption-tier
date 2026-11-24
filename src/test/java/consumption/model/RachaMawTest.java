package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RachaMawTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    private static Libro libro() {
        return new Libro("Titulo", 8.0, Estado.PENDIENTE, "Autor", "Fantasia", 300, 2000);
    }

    @Test
    void sinTerminadosNoHayRacha() {
        assertEquals(0, RachaMaw.dias(List.of(libro()), HOY));
    }

    @Test
    void diasSeguidosDeDistintosItems() {
        Libro a = libro();
        Libro b = libro();
        Libro c = libro();
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(2));
        Transicion.TERMINAR.aplicar(b, HOY.minusDays(1));
        Transicion.TERMINAR.aplicar(c, HOY);
        assertEquals(3, RachaMaw.dias(List.of(a, b, c), HOY));
    }

    @Test
    void laRachaSigueVivaSiHoyAunNoTerminasNada() {
        Libro a = libro();
        Libro b = libro();
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(2));
        Transicion.TERMINAR.aplicar(b, HOY.minusDays(1));
        assertEquals(2, RachaMaw.dias(List.of(a, b), HOY));
    }

    @Test
    void unHuecoCortaLaRacha() {
        Libro a = libro();
        Libro b = libro();
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(3));
        Transicion.TERMINAR.aplicar(b, HOY);
        assertEquals(1, RachaMaw.dias(List.of(a, b), HOY));
    }

    @Test
    void lasRepeticionesSumanDias() {
        Libro a = libro();
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(2));
        Transicion.REPETIR.aplicar(a, HOY.minusDays(2));
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(1));
        Transicion.REPETIR.aplicar(a, HOY.minusDays(1));
        Transicion.TERMINAR.aplicar(a, HOY);
        assertEquals(3, RachaMaw.dias(List.of(a), HOY));
        assertEquals(3, a.getVecesConsumido());
    }

    @Test
    void terminarDosVecesElMismoDiaCuentaUnDia() {
        Libro a = libro();
        Transicion.TERMINAR.aplicar(a, HOY);
        Transicion.REPETIR.aplicar(a, HOY);
        Transicion.TERMINAR.aplicar(a, HOY);
        assertEquals(1, RachaMaw.dias(List.of(a), HOY));
    }

    @Test
    void unItemViejoSinListaConservaSuFechaDeTermino() {
        Libro viejo = libro();
        viejo.setEstado(Estado.TERMINADO);
        viejo.setFechaTermino(HOY.minusDays(1));
        viejo.setVecesConsumido(1);
        assertEquals(List.of(HOY.minusDays(1)), viejo.getFechasTermino());
        assertEquals(1, RachaMaw.dias(List.of(viejo), HOY));
    }

    @Test
    void deshacerUnaTransicionDevuelveLasFechas() {
        Libro a = libro();
        Transicion.TERMINAR.aplicar(a, HOY.minusDays(1));
        ItemConsumption.EstadoAnterior antes = a.capturarEstado();
        Transicion.REPETIR.aplicar(a, HOY);
        Transicion.TERMINAR.aplicar(a, HOY);
        assertEquals(2, RachaMaw.dias(List.of(a), HOY));
        a.restaurarEstado(antes);
        assertEquals(List.of(HOY.minusDays(1)), a.getFechasTermino());
        assertEquals(1, RachaMaw.dias(List.of(a), HOY));
    }

    @Test
    void fechasFuturasNoCuentan() {
        Libro a = libro();
        Transicion.TERMINAR.aplicar(a, HOY.plusDays(1));
        assertEquals(0, RachaMaw.dias(List.of(a), HOY));
    }
}