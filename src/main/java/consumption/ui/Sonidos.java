package consumption.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.ListChangeListener;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

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
    private static final byte[] CLIC = SintesisSonidos.tecla(14, 0.22, 1900);
    private static final byte[] MENU = SintesisSonidos.tecla(10, 0.16, 2600);
    private static final byte[] FILA = SintesisSonidos.tecla(22, 0.20, 520);
    private static final byte[] PELIGRO = secuencia(new double[][] {{330, 40}, {247, 70}});
    private static final byte[] ABANDONAR = secuencia(new double[][] {
            {392, 70}, {0, 25}, {294, 70}, {0, 25}, {196, 170}});
    private static final byte[][] GLITCH = {
            SintesisSonidos.bzz(95, 0.10, 1), SintesisSonidos.bzz(80, 0.10, 2), SintesisSonidos.bzz(110, 0.10, 3)};
    private static final String MARCA = "the-maw.sonidos-instalado";
    private static boolean instalado;
    private static long ultimoGlitch;
    private Sonidos() { }

    public static void guardar() { reproducir(GUARDAR); }

    public static void devorar() { reproducir(DEVORAR); }

    public static void nivel() { reproducir(NIVEL); }

    public static void eliminar() { reproducir(ELIMINAR); }

    public static void clic() { reproducir(CLIC); }

    public static void menu() { reproducir(MENU); }

    public static void fila() { reproducir(FILA); }

    public static void peligro() { reproducir(PELIGRO); }

    public static void abandonar() { reproducir(ABANDONAR); }

    public static void glitch() {
        long ahora = System.nanoTime();
        if (ahora - ultimoGlitch < 200_000_000L) return;
        ultimoGlitch = ahora;
        reproducir(GLITCH[java.util.concurrent.ThreadLocalRandom.current().nextInt(GLITCH.length)]);
    }

    public static void instalar() {
        if (instalado) return;
        instalado = true;
        for (Window ventana : new java.util.ArrayList<>(Window.getWindows())) vigilar(ventana);
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                if (cambio.wasAdded()) for (Window ventana : cambio.getAddedSubList()) vigilar(ventana);
            }
        });
    }

    private static void vigilar(Window ventana) {
        conectar(ventana.getScene());
        ventana.sceneProperty().addListener((obs, antes, ahora) -> conectar(ahora));
    }

    private static void conectar(Scene escena) {
        if (escena == null || Boolean.TRUE.equals(escena.getProperties().get(MARCA))) return;
        escena.getProperties().put(MARCA, Boolean.TRUE);
        escena.addEventFilter(MouseEvent.MOUSE_PRESSED, Sonidos::alPulsar);
    }

    private static void alPulsar(MouseEvent evento) {
        if (!(evento.getTarget() instanceof Node objetivo)) return;
        for (Node actual = objetivo; actual != null; actual = actual.getParent()) {
            if (actual.isDisabled()) return;
            if (actual instanceof ButtonBase || actual instanceof ComboBoxBase<?> || actual instanceof Slider
                    || actual.getStyleClass().contains("column-header")) {
                if (actual.getStyleClass().contains("boton-peligro")) peligro(); else clic();
                return;
            }
            if (actual.getStyleClass().contains("menu-item")) { menu(); return; }
            if (actual instanceof IndexedCell<?> celda) {
                if (!celda.isEmpty()) fila();
                return;
            }
            if (actual.getStyleClass().contains("tarjeta") || actual.getCursor() == Cursor.HAND) {
                fila();
                return;
            }
        }
    }

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