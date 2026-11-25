package consumption.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SintesisSonidosTest {

    private static int pico(byte[] datos) {
        int maximo = 0;
        for (int i = 0; i + 1 < datos.length; i += 2) {
            short muestra = (short) ((datos[i] & 0xFF) | (datos[i + 1] << 8));
            maximo = Math.max(maximo, Math.abs(muestra));
        }
        return maximo;
    }

    @Test void teclaDuraLoPedidoYNoEsSilenciosaNiFuerte() {
        byte[] clic = SintesisSonidos.tecla(14, 0.22, 1900);
        assertEquals((int) (22050 * 14 / 1000.0) * 2, clic.length);
        assertTrue(pico(clic) > 500);
        assertTrue(pico(clic) < 0.3 * Short.MAX_VALUE);
    }

    @Test void zumbidoEsSuaveYCadaVarianteEsDistinta() {
        byte[] a = SintesisSonidos.bzz(95, 0.10, 1);
        byte[] b = SintesisSonidos.bzz(95, 0.10, 2);
        assertTrue(pico(a) > 200);
        assertTrue(pico(a) < 0.15 * Short.MAX_VALUE);
        assertFalse(java.util.Arrays.equals(a, b));
        assertArrayEquals(a, SintesisSonidos.bzz(95, 0.10, 1));
    }
}
