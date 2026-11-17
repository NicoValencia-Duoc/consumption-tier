package consumption.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BorradorFormulario {
    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BorradorFormulario() {}

    public static final class Datos {
        public int version = VERSION;
        public String tipo;
        public Map<String, String> campos = new LinkedHashMap<>();
    }

    private static Path ruta(String tipo) {
        if (!tipo.matches("JUEGO|LIBRO|PELICULA|SERIE")) {
            throw new IllegalArgumentException("Tipo de borrador desconocido");
        }
        return Rutas.BASE.resolve("borrador-" + tipo.toLowerCase(java.util.Locale.ROOT) + ".json");
    }

    public static Optional<Datos> leer(String tipo) {
        Path archivo = ruta(tipo);
        if (!Files.exists(archivo)) return Optional.empty();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            Datos datos = GSON.fromJson(lector, Datos.class);
            if (datos == null || datos.version != VERSION
                    || !tipo.equals(datos.tipo) || datos.campos == null) {
                throw new IllegalStateException("Borrador incompatible");
            }
            return Optional.of(datos);
        } catch (IOException | JsonParseException | IllegalStateException ex) {
            throw new IllegalStateException(
                    "No se pudo leer " + archivo.getFileName() + ": " + ex.getMessage(), ex);
        }
    }

    public static void guardar(String tipo, Map<String, String> campos) {
        Path archivo = ruta(tipo);
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Datos datos = new Datos();
        datos.tipo = tipo;
        datos.campos = new LinkedHashMap<>(campos);
        try {
            Files.createDirectories(archivo.getParent());
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                GSON.toJson(datos, escritor);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el borrador: " + ex.getMessage(), ex);
        }
    }

    public static void borrar(String tipo) {
        try {
            Files.deleteIfExists(ruta(tipo));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo borrar el borrador: " + ex.getMessage(), ex);
        }
    }
}
