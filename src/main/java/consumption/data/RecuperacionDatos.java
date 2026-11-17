package consumption.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class RecuperacionDatos {
    private static final int VERSION_ADMITIDA = 1;
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    private RecuperacionDatos() {}

    public static Optional<Path> ultimoRespaldoLegible() throws IOException {
        List<Path> copias = RespaldoAutomatico.listar();
        return copias.stream()
                .sorted(Comparator.reverseOrder())
                .filter(RecuperacionDatos::esLegible)
                .findFirst();
    }

    private static boolean esLegible(Path zip) {
        try {
            comprobarContenido(leerDatosZip(zip));
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static byte[] leerDatosZip(Path zip) throws IOException {
        try (ZipFile archivo = new ZipFile(zip.toFile())) {
            ZipEntry entrada = archivo.getEntry("datos.json");
            if (entrada == null || entrada.isDirectory()) {
                throw new IOException("El ZIP no contiene datos.json.");
            }
            try (var entradaDatos = archivo.getInputStream(entrada)) {
                return entradaDatos.readAllBytes();
            }
        }
    }

    private static void comprobarContenido(byte[] bytes) throws IOException {
        try (Reader lector = new InputStreamReader(
                new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(lector);
            if (!raiz.isJsonObject()) throw new IOException("Raiz JSON invalida.");
            JsonObject objeto = raiz.getAsJsonObject();
            if (objeto.has("versionFormato")) {
                JsonElement v = objeto.get("versionFormato");
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) {
                    throw new IOException("Version de formato invalida.");
                }
                int version = v.getAsInt();
                if (version < 0 || version > VERSION_ADMITIDA) {
                    throw new IOException("Version de formato incompatible.");
                }
            }
            for (String nombre : List.of("juegos", "libros", "peliculas", "series")) {
                if (objeto.has(nombre) && !objeto.get(nombre).isJsonArray()) {
                    throw new IOException("Categoria invalida: " + nombre);
                }
            }
        } catch (RuntimeException ex) {
            throw new IOException("JSON no legible en respaldo.", ex);
        }
    }

    public static Path restaurarSoloDatos(Path zip) throws IOException {
        byte[] bytes = leerDatosZip(zip);
        comprobarContenido(bytes);
        Files.createDirectories(Rutas.BASE);

        Path copiaEmergencia = null;
        if (Files.exists(Rutas.DATOS)) {
            String marca = FECHA.format(LocalDateTime.now());
            copiaEmergencia = Rutas.BASE.resolve("datos-antes-recuperacion-" + marca + ".json");
            Files.copy(Rutas.DATOS, copiaEmergencia);
        }

        Path temporal = Files.createTempFile(Rutas.BASE, "recuperacion-", ".tmp");
        try {
            Files.write(temporal, bytes);
            mover(temporal, Rutas.DATOS);
        } finally {
            Files.deleteIfExists(temporal);
        }
        return copiaEmergencia;
    }

    public static void revertir(Path copiaEmergencia) throws IOException {
        if (copiaEmergencia == null) {
            Files.deleteIfExists(Rutas.DATOS);
            return;
        }
        Path temporal = Files.createTempFile(Rutas.BASE, "reversion-", ".tmp");
        try {
            Files.copy(copiaEmergencia, temporal, StandardCopyOption.REPLACE_EXISTING);
            mover(temporal, Rutas.DATOS);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static void mover(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}