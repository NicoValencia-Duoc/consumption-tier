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
            return BarreraEscritura.conBloqueo(() -> {
                if (Files.exists(Rutas.BASE.resolve("diario-restauracion-c14/active")))
                    return false;
                if (!Files.exists(Rutas.DATOS)) return false;
                Files.createDirectories(Rutas.RESPALDOS);
                borrarTemporales();
                List<Path> previas = listar();
                if (!previas.isEmpty() && mismoDatos(previas.get(previas.size() - 1))) {
                    // Reparar retencion pendiente incluso sin cambios de biblioteca.
                    podar();
                    return false;
                }
                Path destino = destinoDisponible(ahora);
                Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
                try {
                    RespaldoZip.crear(temporal);
                    falloPrueba("afterCreate");
                    try { Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE); }
                    catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temporal, destino); }
                } finally { Files.deleteIfExists(temporal); }
                falloPrueba("afterPublish");
                podar();
                return true;
            });
        } catch (Exception e) {
            System.err.println("No se pudo crear el respaldo automatico: " + e.getMessage());
            return false;
        }
    }

    private static Path destinoDisponible(LocalDateTime ahora) throws IOException {
        String base = PREFIJO + FORMATO.format(ahora);
        int siguiente = 0;
        for (Path previo : listar()) {
            String n = previo.getFileName().toString();
            if (n.startsWith(base + "~") && n.endsWith(SUFIJO)) {
                String indice = n.substring((base + "~").length(), n.length() - SUFIJO.length());
                if (indice.matches("[0-9]{4}")) siguiente = Math.max(siguiente, Integer.parseInt(indice) + 1);
            }
        }
        for (int i = siguiente; i < 10000; i++) {
            String nombre = base + String.format("~%04d", i) + SUFIJO;
            Path candidato = Rutas.RESPALDOS.resolve(nombre);
            if (!Files.exists(candidato)) return candidato;
        }
        throw new IOException("No hay nombre disponible para respaldo automatico");
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

    private static void falloPrueba(String fase) throws IOException {
        if (fase.equals(System.getProperty("maw.b03.autoFail")))
            throw new IOException("Fallo inyectado en " + fase);
        if (fase.equals(System.getProperty("maw.b03.autoHalt")))
            Runtime.getRuntime().halt(79);
    }

    private static void podar() throws IOException {
        List<Path> copias = listar();
        for (int i = 0; i < copias.size() - MAXIMO; i++) {
            falloPrueba("duringPrune");
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