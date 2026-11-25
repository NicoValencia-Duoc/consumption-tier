package consumption.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Sonidos {
    private static final float TASA = 22050f;
    private static final double VOLUMEN = 0.22;
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "maw-sonidos");
        hilo.setDaemon(true);
        return hilo;
    });

    private static final byte[] GUARDAR = secuencia(new double[][] {{880, 45}});
    private static final byte[] DEVORAR = secuencia(new double[][] {{660, 60}, {0, 30}, {990, 90}});
    private static final byte[] NIVEL = secuencia(new double[][] {{523, 90}, {659, 90}, {784, 90}, {1047, 220}});
    private static final byte[] ELIMINAR = secuencia(new double[][] {{196, 170}});

    private Sonidos() { }

    public static void guardar() { reproducir(GUARDAR); }

    public static void devorar() { reproducir(DEVORAR); }

    public static void nivel() { reproducir(NIVEL); }

    public static void eliminar() { reproducir(ELIMINAR); }

    static byte[] secuencia(double[][] tonos) {
        int total = 0;
        for (double[] tono : tonos) total += (int) (TASA * tono[1] / 1000.0);
        byte[] datos = new byte[total * 2];
        int posicion = 0;
        for (double[] tono : tonos) {
            int muestras = (int) (TASA * tono[1] / 1000.0);
            for (int i = 0; i < muestras; i++) {
                double valor = 0;
                if (tono[0] > 0) {
                    double fase = 2 * Math.PI * tono[0] * i / TASA;
                    double onda = 0.5 * Math.signum(Math.sin(fase)) + 0.5 * Math.sin(fase);
                    double ataque = Math.min(1.0, i / (TASA * 0.003));
                    double caida = Math.pow(1.0 - (double) i / muestras, 1.5);
                    valor = onda * ataque * caida * VOLUMEN;
                }
                short muestra = (short) Math.round(valor * Short.MAX_VALUE);
                datos[posicion++] = (byte) (muestra & 0xFF);
                datos[posicion++] = (byte) ((muestra >> 8) & 0xFF);
            }
        }
        return datos;
    }

    private static void reproducir(byte[] datos) {
        if (!Efectos.sonidosPermitidos()) return;
        HILO.execute(() -> {
            try {
                AudioFormat formato = new AudioFormat(TASA, 16, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(formato, datos, 0, datos.length);
                clip.addLineListener(evento -> {
                    if (evento.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (Exception | LinkageError ex) {}
        });
    }
}