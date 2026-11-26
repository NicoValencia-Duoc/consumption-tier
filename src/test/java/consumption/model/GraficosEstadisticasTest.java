package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraficosEstadisticasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 26);

    private Libro libro(String titulo, double nota, Estado estado, String genero) {
        return new Libro(titulo, nota, estado, "Autor", genero, 100, 2000);
    }

    private Libro cerrado(String titulo, double nota, Estado estado, LocalDate fin) {
        Libro l = libro(titulo, nota, estado, "Drama");
        l.setFechaTermino(fin);
        return l;
    }

    @Test
    void tiersCuentaPorLetraYDaPorcentajeSobreLosQueTienenNota() {
        List<Libro> lista = List.of(libro("a", 9.6, Estado.TERMINADO, "x"), libro("b", 9.0, Estado.TERMINADO, "x"),
                libro("c", 9.1, Estado.TERMINADO, "x"), libro("d", 7.6, Estado.TERMINADO, "x"),
                libro("p", 0, Estado.PENDIENTE, "x"));
        GraficosEstadisticas.Distribucion d = GraficosEstadisticas.distribucionTiers(lista);
        assertEquals(4, d.total());
        assertEquals(6, d.barras().size());
        assertEquals(Tier.S, d.barras().get(0).tier());
        assertEquals(1, d.barras().get(0).cantidad());
        assertEquals(2, d.barras().get(1).cantidad());
        assertTrue(Math.abs(d.barras().get(1).porcentaje() - 50.0) < 1e-9);
        assertEquals(1, d.barras().get(2).cantidad());
        assertEquals(0, d.barras().get(5).cantidad());
    }

    @Test
    void tiersSinNotasDaSeisBarrasEnCero() {
        GraficosEstadisticas.Distribucion d = GraficosEstadisticas.distribucionTiers(List.of());
        assertEquals(0, d.total());
        assertEquals(6, d.barras().size());
        assertTrue(d.barras().get(0).porcentaje() == 0);
    }

    @Test
    void generosAgrupaSinTildesNiMayusculasOrdenaYExcluyePendientes() {
        List<Libro> lista = List.of(libro("1", 5, Estado.TERMINADO, "Acción"), libro("2", 5, Estado.TERMINADO, "accion"),
                libro("3", 5, Estado.TERMINADO, "RPG"), libro("5", 0, Estado.PENDIENTE, "RPG"));
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.rankingGeneros(lista, 10);
        assertEquals(2, r.size());
        assertEquals(2, r.get(0).valor());
        assertEquals("ACCIÓN", r.get(0).etiqueta());
        assertEquals("RPG", r.get(1).etiqueta());
        assertEquals(1, r.get(1).valor());
    }

    @Test
    void generosRespetaElTopeYDesempataAlfabeticamente() {
        List<Libro> lista = List.of(libro("1", 5, Estado.TERMINADO, "B"), libro("2", 5, Estado.TERMINADO, "A"),
                libro("3", 5, Estado.TERMINADO, "C"));
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.rankingGeneros(lista, 2);
        assertEquals(2, r.size());
        assertEquals("A", r.get(0).etiqueta());
        assertEquals("B", r.get(1).etiqueta());
    }

    @Test
    void aniosUsaLaFechaDeTerminoDeTerminadosYAbandonados() {
        List<Libro> lista = List.of(
                cerrado("a", 8, Estado.TERMINADO, LocalDate.of(2025, 3, 1)),
                cerrado("b", 8, Estado.TERMINADO, LocalDate.of(2025, 12, 31)),
                cerrado("c", 8, Estado.ABANDONADO, LocalDate.of(2026, 1, 2)),
                libro("d", 8, Estado.TERMINADO, "x"),
                libro("e", 0, Estado.PENDIENTE, "x"));
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.porAnio(lista, 8);
        assertEquals(2, r.size());
        assertEquals("2025", r.get(0).etiqueta());
        assertEquals(2, r.get(0).valor());
        assertEquals("2026", r.get(1).etiqueta());
        assertEquals(1, r.get(1).valor());
    }

    @Test
    void aniosDejaSoloLosUltimosConDatos() {
        List<Libro> lista = List.of(
                cerrado("a", 8, Estado.TERMINADO, LocalDate.of(2020, 1, 1)),
                cerrado("b", 8, Estado.TERMINADO, LocalDate.of(2022, 1, 1)),
                cerrado("c", 8, Estado.TERMINADO, LocalDate.of(2024, 1, 1)));
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.porAnio(lista, 2);
        assertEquals(2, r.size());
        assertEquals("2022", r.get(0).etiqueta());
        assertEquals("2024", r.get(1).etiqueta());
    }

    @Test
    void mesesSiempreDoceDesdeElMasAntiguoHastaElActual() {
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.porMes(List.of(), HOY);
        assertEquals(12, r.size());
        assertEquals("DIC", r.get(0).etiqueta());
        assertEquals("NOV", r.get(11).etiqueta());
    }

    @Test
    void mesesCuentaPorMesYDescartaLoQueQuedaFuera() {
        List<Libro> lista = List.of(
                cerrado("a", 8, Estado.TERMINADO, LocalDate.of(2026, 11, 3)),
                cerrado("b", 8, Estado.TERMINADO, LocalDate.of(2026, 11, 30)),
                cerrado("c", 8, Estado.TERMINADO, LocalDate.of(2025, 12, 1)),
                cerrado("d", 8, Estado.TERMINADO, LocalDate.of(2025, 11, 30)),
                cerrado("e", 8, Estado.TERMINADO, LocalDate.of(2027, 1, 1)));
        List<GraficosEstadisticas.Barra> r = GraficosEstadisticas.porMes(lista, HOY);
        assertEquals(2, r.get(11).valor());
        assertEquals(1, r.get(0).valor());
        int suma = 0;
        for (GraficosEstadisticas.Barra b : r) suma += b.valor();
        assertEquals(3, suma);
    }
}
