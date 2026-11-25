package consumption.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preferencias {

    public static final int MAX_NOMBRE = 16;
    public static final String NOMBRE_INICIAL = "Sacrificio";

    private double volumen = 0.5;
    private boolean noche = true;
    private String tema;
    private String nombreUsuario = NOMBRE_INICIAL;
    private boolean bienvenidaVista = false;
    private String huellaRechazadosVistos = "";

    public boolean rechazadosYaVistos(String huella) {
        return huella != null && !huella.isBlank() && huella.equals(huellaRechazadosVistos);
    }
    public void marcarRechazadosVistos(String huella) {
        if (huella == null || huella.isBlank()) throw new IllegalArgumentException("Huella vacia");
        huellaRechazadosVistos = huella;
    }

    private boolean tablaCompacta = false;
    private boolean tarjetasCompactas = false;
    private int escalaTexto = 100;
    private boolean efectosActivos = true;
    private boolean efectoCrt = true;
    private boolean efectoEscaneo = true;
    private boolean efectoGlitch = true;
    private boolean efectoArranque = true;
    private boolean efectoAcciones = true;
    private boolean efectoSonidos = true;

    public boolean isTablaCompacta() { return tablaCompacta; }
    public void setTablaCompacta(boolean v) { tablaCompacta = v; }
    public boolean isTarjetasCompactas() { return tarjetasCompactas; }
    public void setTarjetasCompactas(boolean v) { tarjetasCompactas = v; }
    public boolean isEfectosActivos() { return efectosActivos; }
    public void setEfectosActivos(boolean v) { efectosActivos = v; }
    public boolean isEfectoCrt() { return efectoCrt; }
    public void setEfectoCrt(boolean v) { efectoCrt = v; }
    public boolean isEfectoEscaneo() { return efectoEscaneo; }
    public void setEfectoEscaneo(boolean v) { efectoEscaneo = v; }
    public boolean isEfectoGlitch() { return efectoGlitch; }
    public void setEfectoGlitch(boolean v) { efectoGlitch = v; }
    public boolean isEfectoArranque() { return efectoArranque; }
    public void setEfectoArranque(boolean v) { efectoArranque = v; }
    public boolean isEfectoAcciones() { return efectoAcciones; }
    public void setEfectoAcciones(boolean v) { efectoAcciones = v; }
    public boolean isEfectoSonidos() { return efectoSonidos; }
    public void setEfectoSonidos(boolean v) { efectoSonidos = v; }
    public int getEscalaTexto() { return Math.max(90, Math.min(115, escalaTexto == 0 ? 100 : escalaTexto)); }
    public void setEscalaTexto(int v) { escalaTexto = Math.max(90, Math.min(115, v)); }

    private boolean ventanaGuardada = false;
    private double ventanaX;
    private double ventanaY;
    private double ventanaAncho;
    private double ventanaAlto;

    public double getVolumen() { return volumen; }
    public String getTema() {
        if (tema != null && !tema.isBlank()) return tema;
        return noche ? "INDUSTRIAL_NOCTURNO" : "INDUSTRIAL_DIURNO";
    }
    public String getNombreUsuario() { return nombreUsuario; }
    public boolean isBienvenidaVista() { return bienvenidaVista; }

    public boolean isVentanaGuardada() { return ventanaGuardada; }
    public double getVentanaX() { return ventanaX; }
    public double getVentanaY() { return ventanaY; }
    public double getVentanaAncho() { return ventanaAncho; }
    public double getVentanaAlto() { return ventanaAlto; }

    public void setVolumen(double v) { this.volumen = Double.isFinite(v) ? Math.max(0, Math.min(1, v)) : 0.5; }
    public void setTema(String t) {
        this.tema = t;
        this.noche = !"INDUSTRIAL_DIURNO".equals(t) && !"SOLAR".equals(t);
    }
    public void setNombreUsuario(String n) { this.nombreUsuario = limpiarNombre(n); }
    public void setBienvenidaVista(boolean v) { this.bienvenidaVista = v; }

    public void setVentana(double x, double y, double ancho, double alto) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(ancho) || !Double.isFinite(alto)) return;
        this.ventanaX = x;
        this.ventanaY = y;
        this.ventanaAncho = ancho;
        this.ventanaAlto = alto;
        this.ventanaGuardada = true;
    }

    private static final List<String> PESTANAS =
            List.of("INICIO", "JUEGOS", "LIBROS", "PELICULAS", "SERIES", "PENDIENTES", "ESTADISTICAS");

    private String ultimaPestana = "INICIO";
    private Map<String, EstadoTabla> tablas = new HashMap<>();

    public String getUltimaPestana() {
        return PESTANAS.contains(ultimaPestana) ? ultimaPestana : "INICIO";
    }

    public void setUltimaPestana(String pestana) {
        if (PESTANAS.contains(pestana)) this.ultimaPestana = pestana;
    }

    public EstadoTabla estadoTabla(String clave) {
        if (tablas == null) tablas = new HashMap<>();
        return tablas.computeIfAbsent(clave, k -> new EstadoTabla());
    }

    public static String limpiarNombre(String n) {
        String t = (n == null) ? "" : n.trim();
        if (t.isEmpty()) return NOMBRE_INICIAL;
        if (t.length() > MAX_NOMBRE) t = t.substring(0, MAX_NOMBRE).trim();
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    public static Preferencias cargar() {
        if (Files.exists(Rutas.PREFERENCIAS)) {
            try (Reader lector = Files.newBufferedReader(Rutas.PREFERENCIAS, StandardCharsets.UTF_8)) {
                Preferencias leidas = new Gson().fromJson(lector, Preferencias.class);
                if (leidas != null) {
                    leidas.setVolumen(leidas.volumen);
                    leidas.setEscalaTexto(leidas.escalaTexto == 0 ? 100 : leidas.escalaTexto);
                    leidas.setNombreUsuario(leidas.nombreUsuario);
                    return leidas;
                }
                throw new IllegalStateException("preferencias.json no contiene preferencias validas.");
            } catch (IOException | JsonParseException | IllegalStateException e) {
                throw new IllegalStateException("No se pudo cargar preferencias.json. "
                        + "El archivo original se ha conservado sin cambios.", e);
            }
        }
        return new Preferencias();
    }

    public void guardar() {
        BarreraEscritura.escritura(() -> {
            try {
                Files.createDirectories(Rutas.BASE);
                Path tmp = Files.createTempFile(Rutas.BASE, "preferencias-", ".tmp");
                try {
                    Files.writeString(tmp, new Gson().toJson(this), StandardCharsets.UTF_8);
                    try { Files.move(tmp, Rutas.PREFERENCIAS, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                    catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(tmp, Rutas.PREFERENCIAS, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                } finally { Files.deleteIfExists(tmp); }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudieron guardar las preferencias", e);
            }
        });
    }
}