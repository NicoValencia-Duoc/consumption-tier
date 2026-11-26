package consumption.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReglasModeloTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);

    private Libro libro(String titulo, double nota, Estado estado) {
        return new Libro(titulo, nota, estado, "Andrzej Sapkowski", "Fantasia", 320, 1993);
    }

    @Test
    void formatoUsaComaYUnDecimal() {
        assertEquals("8,5", Formato.decimal(8.5));
        assertEquals("9,0", Formato.decimal(9));
        assertEquals("0,0", Formato.decimal(0));
    }

    @Test
    void formatoRedondeaAUnDecimal() {
        assertEquals("7,3", Formato.decimal(7.26));
    }

    @Test
    void tierPorNotaEnLosLimites() {
        assertEquals(Tier.S, Tier.de(10));
        assertEquals(Tier.S, Tier.de(9.5));
        assertEquals(Tier.A, Tier.de(9.4));
        assertEquals(Tier.A, Tier.de(8.5));
        assertEquals(Tier.B, Tier.de(8.4));
        assertEquals(Tier.B, Tier.de(7.5));
        assertEquals(Tier.C, Tier.de(7.4));
        assertEquals(Tier.C, Tier.de(6.0));
        assertEquals(Tier.D, Tier.de(5.9));
        assertEquals(Tier.D, Tier.de(4.0));
        assertEquals(Tier.F, Tier.de(3.9));
        assertEquals(Tier.F, Tier.de(0));
    }

    @Test
    void tierTieneLetraYClaseCss() {
        assertEquals("S", Tier.S.getLetra());
        assertEquals("tier-s", Tier.S.getClaseCss());
        assertEquals("tier-f", Tier.F.getClaseCss());
    }

    @Test
    void normalizarQuitaTildesMayusculasYEspacios() {
        assertEquals("fantasia", ReglasInicio.normalizar("  FANTASÍA "));
        assertEquals("", ReglasInicio.normalizar(null));
    }

    @Test
    void busquedaVaciaCoincideConTodo() {
        assertTrue(ReglasInicio.coincideBusqueda(libro("The Witcher", 8.5, Estado.TERMINADO), "   "));
    }

    @Test
    void busquedaIgnoraTildesYExigeTodasLasPalabras() {
        Libro item = libro("The Witcher", 8.5, Estado.TERMINADO);
        assertTrue(ReglasInicio.coincideBusqueda(item, "fantasía"));
        assertTrue(ReglasInicio.coincideBusqueda(item, "witcher sapkowski"));
        assertFalse(ReglasInicio.coincideBusqueda(item, "witcher dune"));
    }

    @Test
    void pasaTierNuloPasaTodoYPendienteNuncaPasaUnTier() {
        Libro terminado = libro("A", 9.6, Estado.TERMINADO);
        Libro pendiente = libro("B", 0, Estado.PENDIENTE);
        assertTrue(ReglasInicio.pasaTier(terminado, null));
        assertTrue(ReglasInicio.pasaTier(pendiente, null));
        assertTrue(ReglasInicio.pasaTier(terminado, Tier.S));
        assertFalse(ReglasInicio.pasaTier(terminado, Tier.A));
        assertFalse(ReglasInicio.pasaTier(pendiente, Tier.S));
    }

    @Test
    void diasEnCursoSoloParaEnProgresoConFecha() {
        Libro item = libro("A", 0, Estado.PENDIENTE);
        item.setEstado(Estado.EN_PROGRESO);
        assertTrue(ReglasInicio.diasEnCurso(item, HOY) == null);
        item.setFechaInicio(HOY.minusDays(3));
        assertEquals(3, (int) ReglasInicio.diasEnCurso(item, HOY));
        item.setFechaInicio(HOY.plusDays(2));
        assertTrue(ReglasInicio.diasEnCurso(item, HOY) == null);
    }

    @Test
    void textoDiasSegunLaCantidad() {
        Libro item = libro("A", 0, Estado.PENDIENTE);
        item.setEstado(Estado.EN_PROGRESO);
        item.setFechaInicio(HOY);
        assertEquals("EMPEZO HOY", ReglasInicio.textoDias(item, HOY));
        item.setFechaInicio(HOY.minusDays(1));
        assertEquals("LLEVA 1 DIA", ReglasInicio.textoDias(item, HOY));
        item.setFechaInicio(HOY.minusDays(5));
        assertEquals("LLEVA 5 DIAS", ReglasInicio.textoDias(item, HOY));
        item.setEstado(Estado.TERMINADO);
        assertTrue(ReglasInicio.textoDias(item, HOY) == null);
    }

    @Test
    void pendientesVanAlFinalYEnOrdenAlfabetico() {
        Libro b = libro("Beta", 5, Estado.TERMINADO);
        Libro a = libro("alfa", 9, Estado.TERMINADO);
        Libro pz = libro("Zeta", 0, Estado.PENDIENTE);
        Libro pa = libro("Aaa", 0, Estado.PENDIENTE);
        List<ItemConsumption> lista = new ArrayList<>(List.of(pz, b, pa, a));
        lista.sort(ReglasInicio.pendientesAlFinal(
                java.util.Comparator.comparingDouble(ItemConsumption::getNota).reversed()));
        assertEquals(List.of(a, b, pa, pz), lista);
    }

    @Test
    void resumenCuentaPorEstado() {
        List<Libro> lista = List.of(libro("A", 9, Estado.TERMINADO), libro("B", 7, Estado.TERMINADO),
                libro("C", 0, Estado.EN_PROGRESO), libro("D", 3, Estado.ABANDONADO), libro("E", 0, Estado.PENDIENTE));
        Estadisticas.Resumen r = Estadisticas.resumir("LIBROS", lista);
        assertEquals("LIBROS", r.nombre());
        assertEquals(5, r.total());
        assertEquals(2, r.terminados());
        assertEquals(1, r.enProgreso());
        assertEquals(1, r.abandonados());
        assertEquals(1, r.pendientes());
    }

    @Test
    void resumenPromediaSoloConNotaYSinPendientes() {
        List<Libro> lista = List.of(libro("A", 9, Estado.TERMINADO), libro("B", 7, Estado.TERMINADO),
                libro("C", 0, Estado.EN_PROGRESO));
        Estadisticas.Resumen r = Estadisticas.resumir("LIBROS", lista);
        assertTrue(Math.abs(r.notaPromedio() - 8.0) < 1e-9);
        assertEquals("A (9,0)", r.mejor());
    }

    @Test
    void resumenSinNotasDaCeroYGuion() {
        Estadisticas.Resumen vacio = Estadisticas.resumir("LIBROS", List.of());
        assertEquals(0, vacio.total());
        assertTrue(vacio.notaPromedio() == 0);
        assertEquals("-", vacio.mejor());
        Estadisticas.Resumen soloPendiente = Estadisticas.resumir("LIBROS", List.of(libro("A", 0, Estado.PENDIENTE)));
        assertEquals("-", soloPendiente.mejor());
    }
}
