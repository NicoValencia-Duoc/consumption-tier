package consumption.data;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreferenciasEfectosTest {

    @Test
    void unArchivoAntiguoSinCamposDeEfectosLosDejaActivos() {
        Preferencias p = new Gson().fromJson("{\"volumen\":0.3,\"tema\":\"SOLAR\"}", Preferencias.class);
        assertTrue(p.isEfectosActivos());
        assertTrue(p.isEfectoCrt());
        assertTrue(p.isEfectoEscaneo());
        assertTrue(p.isEfectoGlitch());
        assertTrue(p.isEfectoArranque());
    }

    @Test
    void losInterruptoresSobrevivenAlGuardadoYLaLectura() {
        Preferencias p = new Preferencias();
        p.setEfectoGlitch(false);
        p.setEfectosActivos(false);
        Gson gson = new Gson();
        Preferencias leida = gson.fromJson(gson.toJson(p), Preferencias.class);
        assertFalse(leida.isEfectoGlitch());
        assertFalse(leida.isEfectosActivos());
        assertTrue(leida.isEfectoCrt());
    }
}
