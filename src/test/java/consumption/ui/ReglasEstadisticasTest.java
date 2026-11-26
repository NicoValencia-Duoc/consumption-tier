package consumption.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglasEstadisticasTest {

    @Test
    void escalaSeAcotaEntre90y115() {
        assertTrue(Math.abs(ReglasEstadisticas.factorEscala(100) - 1.0) < 1e-9);
        assertTrue(Math.abs(ReglasEstadisticas.factorEscala(115) - 1.15) < 1e-9);
        assertTrue(Math.abs(ReglasEstadisticas.factorEscala(300) - 1.15) < 1e-9);
        assertTrue(Math.abs(ReglasEstadisticas.factorEscala(10) - 0.9) < 1e-9);
    }

    @Test
    void columnasSegunElAncho() {
        assertEquals(1, ReglasEstadisticas.columnas(100, 270, 14, 4));
        assertEquals(2, ReglasEstadisticas.columnas(560, 270, 14, 4));
        assertEquals(4, ReglasEstadisticas.columnas(1200, 270, 14, 4));
        assertEquals(4, ReglasEstadisticas.columnas(5000, 270, 14, 4));
    }

    @Test
    void segmentosPorFraccion() {
        assertEquals(0, ReglasEstadisticas.segmentosPorFraccion(0, 20));
        assertEquals(0, ReglasEstadisticas.segmentosPorFraccion(-1, 20));
        assertEquals(1, ReglasEstadisticas.segmentosPorFraccion(0.01, 20));
        assertEquals(1, ReglasEstadisticas.segmentosPorFraccion(0.042, 20));
        assertEquals(2, ReglasEstadisticas.segmentosPorFraccion(0.083, 20));
        assertEquals(5, ReglasEstadisticas.segmentosPorFraccion(0.229, 20));
        assertEquals(8, ReglasEstadisticas.segmentosPorFraccion(0.375, 20));
        assertEquals(20, ReglasEstadisticas.segmentosPorFraccion(1.0, 20));
        assertEquals(20, ReglasEstadisticas.segmentosPorFraccion(3.0, 20));
    }

    @Test
    void ventanaMinimaAl115NoCortaNiApretaNada() {
        ReglasEstadisticas.Modo m = ReglasEstadisticas.modo(880, 115);
        assertFalse(m.dosSuperiores());
        assertEquals(2, m.columnasMini());
        assertEquals(2, m.columnasTarjetas());
    }

    @Test
    void ventanaMinimaAl90AlcanzaParaDosPanelesArriba() {
        ReglasEstadisticas.Modo m = ReglasEstadisticas.modo(880, 90);
        assertTrue(m.dosSuperiores());
        assertEquals(1, m.columnasMini());
        assertEquals(3, m.columnasTarjetas());
    }

    @Test
    void ventanaAnchaRepiteElMockup() {
        ReglasEstadisticas.Modo m = ReglasEstadisticas.modo(1200, 100);
        assertTrue(m.dosSuperiores());
        assertEquals(2, m.columnasMini());
        assertEquals(4, m.columnasTarjetas());
    }

    @Test
    void ventanaAnchaAl115PasaAUnaColumnaDeMinisYTarjetasDeADos() {
        ReglasEstadisticas.Modo m = ReglasEstadisticas.modo(1200, 115);
        assertTrue(m.dosSuperiores());
        assertEquals(1, m.columnasMini());
        assertEquals(3, m.columnasTarjetas());
    }

    @Test
    void pantallaMuyEstrechaUsaUnaColumna() {
        ReglasEstadisticas.Modo m = ReglasEstadisticas.modo(300, 115);
        assertFalse(m.dosSuperiores());
        assertEquals(1, m.columnasMini());
        assertEquals(1, m.columnasTarjetas());
    }
}
