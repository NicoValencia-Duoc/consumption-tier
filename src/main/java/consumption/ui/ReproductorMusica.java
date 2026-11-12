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

    private final List<Pista> pistas = new ArrayList<>();
    private final List<Pista> cola = new ArrayList<>();
    private int indice = -1;
    private int fallosSeguidos = 0;
    private MediaPlayer jugador;

    private final StringProperty titulo = new SimpleStringProperty("SIN MÚSICA");
    private final BooleanProperty sonando = new SimpleBooleanProperty(false);
    private final DoubleProperty volumen = new SimpleDoubleProperty(0.5);

    public ReproductorMusica() {
        pistas.addAll(leerLista());
        volumen.addListener((obs, antes, ahora) -> {
            if (jugador != null) jugador.setVolume(ahora.doubleValue());
        });
    }

    public StringProperty tituloProperty() { return titulo; }
    public BooleanProperty sonandoProperty() { return sonando; }
    public DoubleProperty volumenProperty() { return volumen; }
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
            sonando.set(true);
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

    public void detener() {
        detenerJugador();
        sonando.set(false);
    }

    private void reproducirActual() {
        detenerJugador();
        Pista pista = cola.get(indice);
        try {
            URL url = ReproductorMusica.class.getResource(CARPETA + pista.archivo());
            if (url == null) throw new IOException("No se encontró " + pista.archivo());

            jugador = new MediaPlayer(new Media(url.toExternalForm()));
            jugador.setVolume(volumen.get());
            jugador.setOnPlaying(() -> fallosSeguidos = 0);
            jugador.setOnEndOfMedia(this::siguiente);
            jugador.setOnError(() -> alFallar());
            jugador.play();

            titulo.set(pista.textoCompleto().toUpperCase());
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
            titulo.set("NO SE PUDO REPRODUCIR LA MÚSICA");
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
            System.err.println("No se pudo leer la lista de música: " + e.getMessage());
        }
        return lista;
    }
}