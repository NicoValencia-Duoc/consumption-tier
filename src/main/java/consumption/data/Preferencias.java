package consumption.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

public class Preferencias {

    private double volumen = 0.5;
    private boolean noche = true;

    public double getVolumen() { return volumen; }
    public boolean isNoche() { return noche; }

    public void setVolumen(double v) { this.volumen = Math.max(0, Math.min(1, v)); }
    public void setNoche(boolean n) { this.noche = n; }

    public static Preferencias cargar() {
        if (Files.exists(Rutas.PREFERENCIAS)) {
            try (Reader lector = Files.newBufferedReader(Rutas.PREFERENCIAS)) {
                Preferencias leidas = new Gson().fromJson(lector, Preferencias.class);
                if (leidas != null) {
                    leidas.setVolumen(leidas.volumen);
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