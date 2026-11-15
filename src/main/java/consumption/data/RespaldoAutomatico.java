package consumption.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class RespaldoAutomatico {

    public static final int MAXIMO = 5;
    private static final String PREFIJO = "auto-";
    private static final String SUFIJO = ".zip";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private RespaldoAutomatico() {
    }

    public static boolean crearSiCambio() {
        return crearSiCambio(LocalDateTime.now());
    }

    public static boolean crearSiCambio(LocalDateTime ahora) {
        try {
            if (!Files.exists(Rutas.DATOS)) return false;
            Files.createDirectories(Rutas.RESPALDOS);
            borrarTemporales();

            List<Path> previas = listar();
            if (!previas.isEmpty() && mismoDatos(previas.get(previas.size() - 1))) return false;

            Path destino = Rutas.RESPALDOS.resolve(PREFIJO + FORMATO.format(ahora) + SUFIJO);
            Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
            RespaldoZip.crear(temporal);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);

            podar();
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo crear el respaldo automatico: " + e.getMessage());
            return false;
        }
    }

    public static List<Path> listar() throws IOException {
        if (!Files.isDirectory(Rutas.RESPALDOS)) return List.of();
        try (Stream<Path> archivos = Files.list(Rutas.RESPALDOS)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(PREFIJO)
                            && p.getFileName().toString().endsWith(SUFIJO))
                    .sorted()
                    .toList();
        }
    }

    private static boolean mismoDatos(Path copia) {
        try (ZipFile zip = new ZipFile(copia.toFile())) {
            ZipEntry entrada = zip.getEntry("datos.json");
            if (entrada == null) return false;
            byte[] guardado = zip.getInputStream(entrada).readAllBytes();
            return Arrays.equals(guardado, Files.readAllBytes(Rutas.DATOS));
        } catch (IOException e) {
            return false;
        }
    }

    private static void podar() throws IOException {
        List<Path> copias = listar();
        for (int i = 0; i < copias.size() - MAXIMO; i++) {
            Files.deleteIfExists(copias.get(i));
        }
    }

    private static void borrarTemporales() throws IOException {
        try (Stream<Path> archivos = Files.list(Rutas.RESPALDOS)) {
            for (Path p : (Iterable<Path>) archivos.filter(x -> x.getFileName().toString().endsWith(".tmp"))::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}