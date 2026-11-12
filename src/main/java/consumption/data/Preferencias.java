package consumption.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

public class Preferencias {

    public static final int MAX_NOMBRE = 16;
    public static final String NOMBRE_INICIAL = "Sacrificio";

    private double volumen = 0.5;
    private boolean noche = true;
    private String nombreUsuario = NOMBRE_INICIAL;
    private boolean bienvenidaVista = false;

    public double getVolumen() { return volumen; }
    public boolean isNoche() { return noche; }
    public String getNombreUsuario() { return nombreUsuario; }
    public boolean isBienvenidaVista() { return bienvenidaVista; }

    public void setVolumen(double v) { this.volumen = Math.max(0, Math.min(1, v)); }
    public void setNoche(boolean n) { this.noche = n; }
    public void setNombreUsuario(String n) { this.nombreUsuario = limpiarNombre(n); }
    public void setBienvenidaVista(boolean v) { this.bienvenidaVista = v; }

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