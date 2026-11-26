package consumption.ui;

import consumption.model.Transicion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReglasAccionesTest {

    @Test
    void textosDeEliminadoYRestauradoSegunCantidad() {
        assertEquals("1 REGISTRO ELIMINADO", ReglasAcciones.textoEliminado(1));
        assertEquals("4 REGISTROS ELIMINADOS", ReglasAcciones.textoEliminado(4));
        assertEquals("REGISTRO RESTAURADO", ReglasAcciones.textoRestaurado(1));
        assertEquals("2 REGISTROS RESTAURADOS", ReglasAcciones.textoRestaurado(2));
    }

    @Test
    void avisoPorTransicion() {
        assertEquals("EMPEZADO", ReglasAcciones.textoAviso(Transicion.EMPEZAR));
        assertEquals("ABANDONADO", ReglasAcciones.textoAviso(Transicion.ABANDONAR));
        assertEquals("TERMINADO", ReglasAcciones.textoAviso(Transicion.TERMINAR));
        assertEquals("REPETIDO", ReglasAcciones.textoAviso(Transicion.REPETIR));
    }

    @Test
    void confirmarUnoNombraElTitulo() {
        String texto = ReglasAcciones.textoConfirmarEliminar(List.of("Dune"));
        assertTrue(texto.startsWith("¿Eliminar «Dune»?"));
        assertTrue(texto.contains(ReglasAcciones.SEGUNDOS_DESHACER + " segundos"));
    }

    @Test
    void confirmarVariosListaHastaElMaximoYCuentaElResto() {
        String texto = ReglasAcciones.textoConfirmarEliminar(List.of("A", "B", "C", "D", "E"));
        assertTrue(texto.startsWith("¿Eliminar 5 registros?"));
        assertTrue(texto.contains("• A") && texto.contains("• C"));
        assertFalse(texto.contains("• D"));
        assertTrue(texto.contains("... y 2 mas"));
    }

    @Test
    void confirmarVariosSinExcederElMaximoNoDiceMas() {
        String texto = ReglasAcciones.textoConfirmarEliminar(List.of("A", "B", "C"));
        assertFalse(texto.contains("mas"));
    }

    @Test
    void titulosLargosSeAcortan() {
        String largo = "x".repeat(80);
        String texto = ReglasAcciones.textoConfirmarEliminar(List.of(largo));
        assertTrue(texto.contains("x".repeat(47) + "..."));
        assertFalse(texto.contains("x".repeat(48)));
    }
}