package consumption.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
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

    public void setVolumen(double v) { this.volumen = Math.max(0, Math.min(1, v)); }
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
            try (Reader lector = Files.newBufferedReader(Rutas.PREFERENCIAS)) {
                Preferencias leidas = new Gson().fromJson(lector, Preferencias.class);
                if (leidas != null) {
                    leidas.setVolumen(leidas.volumen);
                    leidas.setNombreUsuario(leidas.nombreUsuario);
                    return leidas;
                }
            } catch (IOException | JsonParseException e) {
                System.err.println("No se pudieron leer las preferencias: " + e.getMessage());
            }
        }
        return new Preferencias();
    }

    public void guardar() {
        try {
            Files.createDirectories(Rutas.BASE);
            Files.writeString(Rutas.PREFERENCIAS, new Gson().toJson(this));
        } catch (IOException e) {
            System.err.println("No se pudieron guardar las preferencias: " + e.getMessage());
        }
    }
}