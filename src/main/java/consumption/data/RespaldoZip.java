package consumption.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class RespaldoZip {

    private static final String PREFIJO_IMAGENES = "imagenes/";

    public static void crear(Path destino) throws IOException {
        if (!Files.exists(Rutas.DATOS)) {
            throw new IOException("Aun no hay datos guardados para respaldar.");
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destino))) {
            agregar(zip, Rutas.DATOS, "datos.json");
            if (Files.isDirectory(Rutas.IMAGENES)) {
                try (Stream<Path> archivos = Files.list(Rutas.IMAGENES)) {
                    for (Path imagen : (Iterable<Path>) archivos::iterator) {
                        if (Files.isRegularFile(imagen)) {
                            agregar(zip, imagen, PREFIJO_IMAGENES + imagen.getFileName());
                        }
                    }
                }
            }
        }
    }

    private static void agregar(ZipOutputStream zip, Path archivo, String nombre) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        Files.copy(archivo, zip);
        zip.closeEntry();
    }

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    public static Path restaurar(Path origen) throws IOException {
        Map<String, byte[]> contenido = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(origen))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                String nombre = entrada.getName();
                if (entrada.isDirectory() || !esNombreSeguro(nombre)) continue;
                contenido.put(nombre, zip.readAllBytes());
            }
        }
        byte[] datos = contenido.get("datos.json");
        if (datos == null) {
            throw new IOException("El archivo no es un respaldo valido (falta datos.json).");
        }
        RecuperacionDatos.comprobarContenido(datos);

        Files.createDirectories(Rutas.BASE);
        Path copiaEmergencia = null;
        if (Files.exists(Rutas.DATOS)) {
            copiaEmergencia = Rutas.BASE.resolve(
                    "datos-antes-restaurar-" + SELLO.format(LocalDateTime.now()) + ".json");
            Files.copy(Rutas.DATOS, copiaEmergencia);
        }

        Files.createDirectories(Rutas.IMAGENES);
        for (Map.Entry<String, byte[]> e : contenido.entrySet()) {
            if (e.getKey().equals("datos.json")) continue;
            Files.write(Rutas.IMAGENES.resolve(e.getKey().substring(PREFIJO_IMAGENES.length())), e.getValue());
        }

        Path temporal = Files.createTempFile(Rutas.BASE, "restaurar-", ".tmp");
        try {
            Files.write(temporal, datos);
            RecuperacionDatos.mover(temporal, Rutas.DATOS);
        } finally {
            Files.deleteIfExists(temporal);
        }
        return copiaEmergencia;
    }

    private static boolean esNombreSeguro(String nombre) {
        if (nombre.equals("datos.json")) return true;
        return nombre.startsWith(PREFIJO_IMAGENES)
                && nombre.substring(PREFIJO_IMAGENES.length()).matches("[\\w\\-]+\\.(png|jpg|jpeg|gif|bmp)");
    }
}