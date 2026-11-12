package consumption.data;

import consumption.model.ItemConsumption;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GestorImagenes {

    private static final Path CARPETA =
            Rutas.IMAGENES;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, Image> CACHE = new HashMap<>();

    public static String copiar(File origen) {
        String nombre = origen.getName().toLowerCase();
        if (!nombre.matches(".*\\.(png|jpg|jpeg|gif|bmp)")) {
            throw new IllegalArgumentException("La imagen debe ser PNG, JPG, GIF o BMP.");
        }
        try {
            if (Files.size(origen.toPath()) > MAX_BYTES) {
                throw new IllegalArgumentException("La imagen pesa mas de 5 MB.");
            }
            Files.createDirectories(CARPETA);
            String destino = UUID.randomUUID() + nombre.substring(nombre.lastIndexOf('.'));
            Files.copy(origen.toPath(), CARPETA.resolve(destino));
            return destino;
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo copiar la imagen.");
        }
    }

    public static void eliminar(String nombre) {
        if (nombre == null) return;
        CACHE.keySet().removeIf(k -> k.startsWith(nombre + "@"));
        try {
            Files.deleteIfExists(CARPETA.resolve(nombre));
        } catch (IOException ignorada) {
        }
    }

    public static Image imagenDe(ItemConsumption item, double ancho, double alto) {
        String uri;
        String clave;
        if (item.getRutaImagen() != null && Files.exists(CARPETA.resolve(item.getRutaImagen()))) {
            clave = item.getRutaImagen();
            uri = CARPETA.resolve(clave).toUri().toString();
        } else {
            clave = nombreIcono(item.getTipo());
            var recurso = GestorImagenes.class.getResource("/consumption/iconos/" + clave);
            if (recurso == null) return null;
            uri = recurso.toExternalForm();
        }
        return CACHE.computeIfAbsent(clave + "@" + ancho + "x" + alto,
                k -> new Image(uri, ancho, alto, true, true));
    }

    private static String nombreIcono(String tipo) {
        return switch (tipo) {
            case "JUEGOS" -> "juego.png";
            case "LIBROS" -> "libro.png";
            case "SERIES" -> "serie.png";
            default -> "pelicula.png";
        };
    }

    public static void limpiarCache() {
        CACHE.clear();
    }
}
