package consumption.ui;

import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReproductorMusica {

    private static final String CARPETA = "/consumption/musica/";

    private final List<Pista> pistas = new ArrayList<>();
    private final List<Pista> cola = new ArrayList<>();
    private int indice = -1;
    private int fallosSeguidos = 0;
    private MediaPlayer jugador;

    private final StringProperty titulo = new SimpleStringProperty("THE MAW EN SILENCIO");
    private final BooleanProperty sonando = new SimpleBooleanProperty(false);
    private final DoubleProperty volumen = new SimpleDoubleProperty(0.5);
    private final DoubleProperty progreso = new SimpleDoubleProperty(0);
    private final StringProperty tiempo = new SimpleStringProperty("");

    public ReproductorMusica() {
        pistas.addAll(leerLista());
        volumen.addListener((obs, antes, ahora) -> {
            if (jugador != null) jugador.setVolume(ahora.doubleValue());
        });
    }

    public StringProperty tituloProperty() { return titulo; }
    public BooleanProperty sonandoProperty() { return sonando; }
    public DoubleProperty volumenProperty() { return volumen; }
    public ReadOnlyDoubleProperty progresoProperty() { return progreso; }
    public ReadOnlyStringProperty tiempoProperty() { return tiempo; }
    public boolean hayMusica() { return !pistas.isEmpty(); }
    public List<Pista> getPistas() { return List.copyOf(pistas); }

    public Pista getActual() {
        boolean valida = jugador != null && indice >= 0 && indice < cola.size();
        return valida ? cola.get(indice) : null;
    }

    public void iniciar() {
        if (pistas.isEmpty()) return;
        barajar();
        indice = 0;
        reproducirActual();
    }

    public void reproducir(Pista pista) {
        if (cola.isEmpty()) barajar();
        int posicion = cola.indexOf(pista);
        if (posicion < 0) return;
        indice = posicion;
        reproducirActual();
    }

    public void alternarPausa() {
        if (jugador == null) {
            iniciar();
            return;
        }
        if (sonando.get()) {
            jugador.pause();
            sonando.set(false);
        } else {
            jugador.play();
        }
    }

    public void siguiente() {
        if (pistas.isEmpty()) return;
        indice++;
        if (indice >= cola.size()) {
            barajar();
            indice = 0;
        }
        reproducirActual();
    }

    public boolean buscar(double fraccion) {
        if (jugador == null) return false;
        Duration total = jugador.getTotalDuration();
        if (!duracionConocida(total)) return false;
        jugador.seek(total.multiply(Math.max(0, Math.min(1, fraccion))));
        return true;
    }

    public void detener() {
        detenerJugador();
        sonando.set(false);
    }

    private void reproducirActual() {
        detenerJugador();
        Pista pista = cola.get(indice);
        try {
            URL url = ReproductorMusica.class.getResource(CARPETA + pista.archivo());
            if (url == null) throw new IOException("No se encontro " + pista.archivo());

            MediaPlayer nuevo = new MediaPlayer(new Media(url.toExternalForm()));
            jugador = nuevo;
            nuevo.setVolume(volumen.get());
            nuevo.setOnPlaying(() -> {
                if (jugador != nuevo) return;
                fallosSeguidos = 0;
                sonando.set(true);
            });
            nuevo.setOnPaused(() -> {
                if (jugador == nuevo) sonando.set(false);
            });
            nuevo.setOnStopped(() -> {
                if (jugador == nuevo) sonando.set(false);
            });
            nuevo.setOnEndOfMedia(() -> {
                if (jugador == nuevo) siguiente();
            });
            nuevo.setOnError(() -> {
                if (jugador == nuevo) alFallar();
            });
            nuevo.setOnReady(() -> actualizarProgreso(nuevo));
            nuevo.currentTimeProperty().addListener((obs, antes, ahora) -> actualizarProgreso(nuevo));
            nuevo.play();

            titulo.set(pista.textoCompleto().toUpperCase());
            sonando.set(false);
        } catch (Exception e) {
            alFallar();
        }
    }

    private void alFallar() {
        fallosSeguidos++;
        if (cola.isEmpty() || fallosSeguidos >= cola.size()) {
            detenerJugador();
            sonando.set(false);
            titulo.set("NO SE PUDO REPRODUCIR LA MUSICA");
            return;
        }
        siguiente();
    }

    private void actualizarProgreso(MediaPlayer reproduciendo) {
        if (reproduciendo != jugador) return;
        Duration actual = reproduciendo.getCurrentTime();
        Duration total = reproduciendo.getTotalDuration();
        if (duracionConocida(total)) {
            progreso.set(Math.min(1, actual.toMillis() / total.toMillis()));
            tiempo.set(formatear(actual) + " / " + formatear(total));
        } else {
            progreso.set(0);
            tiempo.set(formatear(actual));
        }
    }

    private static boolean duracionConocida(Duration d) {
        return d != null && !d.isUnknown() && !d.isIndefinite() && d.toMillis() > 0;
    }

    static String formatear(Duration d) {
        long segundos = (long) d.toSeconds();
        return (segundos / 60) + ":" + String.format("%02d", segundos % 60);
    }

    private void detenerJugador() {
        progreso.set(0);
        tiempo.set("");
        if (jugador == null) return;
        jugador.setOnEndOfMedia(null);
        jugador.setOnError(null);
        jugador.setOnPlaying(null);
        jugador.setOnPaused(null);
        jugador.setOnStopped(null);
        jugador.setOnReady(null);
        jugador.stop();
        jugador.dispose();
        jugador = null;
    }

    private void barajar() {
        Pista ultima = cola.isEmpty() ? null : cola.get(cola.size() - 1);
        cola.clear();
        cola.addAll(pistas);
        Collections.shuffle(cola);

        if (cola.size() > 1 && cola.get(0).equals(ultima)) Collections.swap(cola, 0, 1);
    }

    private static List<Pista> leerLista() {
        List<Pista> lista = new ArrayList<>();
        try (InputStream in = ReproductorMusica.class.getResourceAsStream(CARPETA + "lista.txt")) {
            if (in == null) return lista;
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.replace("\uFEFF", "").trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                Pista pista = Pista.desdeLinea(linea);
                if (!pista.archivo().isEmpty()) lista.add(pista);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer la lista de musica: " + e.getMessage());
        }
        return lista;
    }
}