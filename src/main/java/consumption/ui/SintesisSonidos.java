package consumption.ui;

import java.util.Random;

final class SintesisSonidos {
    static final float TASA = 22050f;

    private SintesisSonidos() { }

    static byte[] tecla(int milisegundos, double ganancia, double tono) {
        int total = (int) (TASA * milisegundos / 1000.0);
        byte[] datos = new byte[total * 2];
        Random azar = new Random(7);
        double anterior = 0;
        for (int i = 0; i < total; i++) {
            double ruido = azar.nextDouble() * 2 - 1;
            double agudo = (ruido - anterior) * 0.5;
            anterior = ruido;
            double onda = Math.signum(Math.sin(2 * Math.PI * tono * i / TASA));
            double caida = Math.pow(1.0 - (double) i / total, 2.5);
            escribir(datos, i, (0.6 * agudo + 0.4 * onda) * caida * ganancia);
        }
        return datos;
    }

    static byte[] bzz(int milisegundos, double ganancia, long semilla) {
        int total = (int) (TASA * milisegundos / 1000.0);
        byte[] datos = new byte[total * 2];
        Random azar = new Random(semilla);
        boolean abierto = true;
        int resto = 0;
        for (int i = 0; i < total; i++) {
            if (resto <= 0) {
                abierto = azar.nextInt(4) != 0;
                resto = (int) (TASA * (0.002 + azar.nextDouble() * 0.007));
            }
            resto--;
            double base = Math.signum(Math.sin(2 * Math.PI * 95 * i / TASA)) * 0.55
                    + (azar.nextDouble() * 2 - 1) * 0.45;
            double caida = Math.pow(1.0 - (double) i / total, 1.2);
            escribir(datos, i, abierto ? base * caida * ganancia : 0);
        }
        return datos;
    }

    private static void escribir(byte[] datos, int indice, double valor) {
        short muestra = (short) Math.round(Math.max(-1.0, Math.min(1.0, valor)) * Short.MAX_VALUE);
        datos[indice * 2] = (byte) (muestra & 0xFF);
        datos[indice * 2 + 1] = (byte) ((muestra >> 8) & 0xFF);
    }
}
