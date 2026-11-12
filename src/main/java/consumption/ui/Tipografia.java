package consumption.ui;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

public class Tipografia {

    private static final String[] ARCHIVOS = {
            "Orbitron-Regular.ttf", "Orbitron-Bold.ttf",
            "Rajdhani-Regular.ttf", "Rajdhani-Bold.ttf"};

    public static void cargar() {
        for (String archivo : ARCHIVOS) {
            try (InputStream in = Tipografia.class.getResourceAsStream("/consumption/fuentes/" + archivo)) {
                if (in == null) {
                    System.err.println("Falta la fuente: " + archivo);
                    continue;
                }
                Font fuente = Font.loadFont(in, 12);
                System.out.println(fuente == null
                        ? "No se pudo cargar " + archivo
                        : "Fuente cargada: " + fuente.getFamily() + " " + fuente.getStyle());
            } catch (IOException e) {
                System.err.println("Error al leer " + archivo + ": " + e.getMessage());
            }
        }
    }
}