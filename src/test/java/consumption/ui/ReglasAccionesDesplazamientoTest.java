package consumption.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglasAccionesDesplazamientoTest {

    @Test
    void sinRecorridoNoSeMueve() {
        assertTrue(ReglasAcciones.posicionDesplazamiento(300, 100, 500, 500) == 0);
        assertTrue(ReglasAcciones.posicionDesplazamiento(300, 100, 800, 500) == 0);
    }

    @Test
    void elementoArribaVaAlInicioYAbajoAlFinal() {
        assertTrue(ReglasAcciones.posicionDesplazamiento(0, 100, 400, 2000) == 0);
        assertTrue(ReglasAcciones.posicionDesplazamiento(1950, 100, 400, 2000) == 1);
    }

    @Test
    void elementoDelMedioQuedaCentrado() {
        double v = ReglasAcciones.posicionDesplazamiento(1000, 100, 400, 2000);
        assertTrue(Math.abs(v - 850.0 / 1600.0) < 1e-9);
    }
}