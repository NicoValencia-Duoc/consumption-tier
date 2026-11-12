package consumption.ui;

import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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

    private final List<String> archivos = new ArrayList<>();
    private final List<String> cola = new ArrayList<>();
    private int indice = -1;
    private int fallosSeguidos = 0;
    private MediaPlayer jugador;

    private final StringProperty titulo = new SimpleStringProperty("SIN MUSICA");
    private final BooleanProperty sonando = new SimpleBooleanProperty(false);
    private final DoubleProperty volumen = new SimpleDoubleProperty(0.5);

    public ReproductorMusica() {
        archivos.addAll(leerLista());
        volumen.addListener((obs, antes, ahora) -> {
            if (jugador != null) jugador.setVolume(ahora.doubleValue());
        });
    }

    public StringProperty tituloProperty() { return titulo; }
    public BooleanProperty sonandoProperty() { return sonando; }
    public DoubleProperty volumenProperty() { return volumen; }
    public boolean hayMusica() { return !archivos.isEmpty(); }

    public void iniciar() {
        if (archivos.isEmpty()) return;
        barajar();
        indice = 0;
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
            sonando.set(true);
        }
    }

    public void siguiente() {
        if (archivos.isEmpty()) return;
        indice++;
        if (indice >= cola.size()) {
            barajar();
            indice = 0;
        }
        reproducirActual();
    }

    public void detener() {
        detenerJugador();
        sonando.set(false);
    }

    private void reproducirActual() {
        detenerJugador();
        String archivo = cola.get(indice);
        try {
            URL url = ReproductorMusica.class.getResource(CARPETA + archivo);
            if (url == null) throw new IOException("No se encontro " + archivo);

            jugador = new MediaPlayer(new Media(url.toExternalForm()));
            jugador.setVolume(volumen.get());
            jugador.setOnPlaying(() -> fallosSeguidos = 0);
            jugador.setOnEndOfMedia(this::siguiente);
            jugador.setOnError(() -> alFallar());
            jugador.play();

            titulo.set(nombreLegible(archivo));
            sonando.set(true);
        } catch (Exception e) {
            alFallar();
        }
    }

    private void alFallar() {
        fallosSeguidos++;
        if (fallosSeguidos >= cola.size()) {
            detenerJugador();
            sonando.set(false);
            titulo.set("NO SE PUDO REPRODUCIR LA MUSICA");
            return;
        }
        siguiente();
    }

    private void detenerJugador() {
        if (jugador == null) return;
        jugador.setOnEndOfMedia(null);
        jugador.setOnError(null);
        jugador.setOnPlaying(null);
        jugador.stop();
        jugador.dispose();
        jugador = null;
    }

    private void barajar() {
        String ultima = cola.isEmpty() ? null : cola.get(cola.size() - 1);
        cola.clear();
        cola.addAll(archivos);
        Collections.shuffle(cola);
        if (cola.size() > 1 && cola.get(0).equals(ultima)) Collections.swap(cola, 0, 1);
    }

    private static String nombreLegible(String archivo) {
        int punto = archivo.lastIndexOf('.');
        String sinExtension = (punto > 0) ? archivo.substring(0, punto) : archivo;
        return sinExtension.replace('_', ' ').toUpperCase();
    }

    private static List<String> leerLista() {
        List<String> lista = new ArrayList<>();
        try (InputStream in = ReproductorMusica.class.getResourceAsStream(CARPETA + "lista.txt")) {
            if (in == null) return lista;
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.replace("\uFEFF", "").trim();
                if (!linea.isEmpty() && !linea.startsWith("#")) lista.add(linea);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer la lista de musica: " + e.getMessage());
        }
        return lista;
    }
}