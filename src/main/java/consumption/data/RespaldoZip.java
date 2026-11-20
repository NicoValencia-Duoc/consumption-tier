package consumption.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.LinkOption;
import java.util.HashSet;
import java.util.Set;
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
        RecuperacionDatos.ejecutarProtegido(() -> { crearInterno(destino); return null; });
    }

    private static void crearInterno(Path destino) throws IOException {
        if (!Files.exists(Rutas.DATOS)) {
            throw new IOException("Aun no hay datos guardados para respaldar.");
        }
        Path parent = destino.toAbsolutePath().getParent();
        if (parent == null) throw new IOException("Destino ZIP sin directorio.");
        Files.createDirectories(parent);
        Path temporal = Files.createTempFile(parent, "maw-respaldo-", ".tmp");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporal))) {
                agregar(zip, Rutas.DATOS, "datos.json");
                if (Files.isDirectory(Rutas.IMAGENES)) {
                    try (Stream<Path> archivos = Files.list(Rutas.IMAGENES)) {
                        for (Path imagen : (Iterable<Path>) archivos::iterator) {
                            String nombre = PREFIJO_IMAGENES + imagen.getFileName();
                            if (Files.isRegularFile(imagen, LinkOption.NOFOLLOW_LINKS) && esNombreSeguro(nombre))
                                agregar(zip, imagen, nombre);
                        }
                    }
                }
            }
            RecuperacionDatos.mover(temporal, destino);
        } finally { Files.deleteIfExists(temporal); }
    }

    private static void agregar(ZipOutputStream zip, Path archivo, String nombre) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        Files.copy(archivo, zip);
        zip.closeEntry();
    }

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    public static Path restaurar(Path origen) throws IOException {
        return RecuperacionDatos.ejecutarProtegido(() -> {
            DiarioZipImagenes.recuperarPendiente();
            return restaurarInterno(origen);
        });
    }

    private static Path restaurarInterno(Path origen) throws IOException {
        Map<String, byte[]> contenido = new LinkedHashMap<>();
        Set<String> nombres = new HashSet<>();
        long total = 0;
        final long MAX_ENTRADA = 64L * 1024 * 1024;
        final long MAX_TOTAL = 512L * 1024 * 1024;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(origen))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                String nombre = entrada.getName();
                if (entrada.isDirectory() || !esNombreSeguro(nombre))
                    throw new IOException("Entrada ZIP no permitida: " + nombre);
                if (!nombres.add(nombre)) throw new IOException("Entrada ZIP duplicada: " + nombre);
                if (nombres.size() > 10000) throw new IOException("Demasiadas entradas ZIP.");
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zip.read(buffer)) != -1) {
                    if (bytes.size() + (long)n > MAX_ENTRADA || total + n > MAX_TOTAL)
                        throw new IOException("Respaldo excede los limites admitidos.");
                    bytes.write(buffer, 0, n);
                    total += n;
                }
                contenido.put(nombre, bytes.toByteArray());
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
        java.util.List<Path> objetivos = new java.util.ArrayList<>();
        objetivos.add(Rutas.DATOS);
        objetivos.add(Rutas.PAPELERA);
        for (String nombre : contenido.keySet()) {
            if (!nombre.equals("datos.json")) {
                Path imagen = Rutas.IMAGENES.resolve(nombre.substring(PREFIJO_IMAGENES.length()));
                if (Files.isSymbolicLink(imagen)) throw new IOException("Imagen destino es enlace simbolico.");
                objetivos.add(imagen);
            }
        }
        DiarioZipImagenes.iniciar(objetivos);
        Map<Path, byte[]> anteriores = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, byte[]> e : contenido.entrySet()) {
                if (e.getKey().equals("datos.json")) continue;
                Path imagen = Rutas.IMAGENES.resolve(e.getKey().substring(PREFIJO_IMAGENES.length()));
                if (Files.isSymbolicLink(imagen)) throw new IOException("Imagen destino es enlace simbolico.");
                anteriores.put(imagen, Files.exists(imagen) ? Files.readAllBytes(imagen) : null);
                escribirAtomico(imagen, e.getValue());
                if (Boolean.getBoolean("maw.b03.haltAfterImage")) Runtime.getRuntime().halt(73);
                if (Boolean.getBoolean("maw.b03.failAfterImage")) throw new IOException("Fallo inyectado despues de imagen");
            }
            escribirAtomico(Rutas.DATOS, datos);
            if (Boolean.getBoolean("maw.b03.haltAfterJson")) Runtime.getRuntime().halt(74);
            DiarioZipImagenes.confirmar();
        } catch (IOException | RuntimeException fallo) {
            IOException rollbackError = null;
            for (Map.Entry<Path, byte[]> anterior : anteriores.entrySet()) {
                try {
                    if (anterior.getValue() == null) Files.deleteIfExists(anterior.getKey());
                    else escribirAtomico(anterior.getKey(), anterior.getValue());
                } catch (IOException ex) {
                    if (rollbackError == null) rollbackError = ex;
                    else rollbackError.addSuppressed(ex);
                }
            }
            try { DiarioZipImagenes.recuperarPendiente(); }
            catch (IOException ex) { fallo.addSuppressed(ex); }
            if (rollbackError != null) fallo.addSuppressed(rollbackError);
            throw fallo;
        }
        return copiaEmergencia;
    }

    private static void escribirAtomico(Path destino, byte[] bytes) throws IOException {
        Path temporal = Files.createTempFile(destino.getParent(), "maw-restaurar-", ".tmp");
        try {
            Files.write(temporal, bytes);
            RecuperacionDatos.mover(temporal, destino);
        } finally { Files.deleteIfExists(temporal); }
    }

    private static boolean esNombreSeguro(String nombre) {
        if (nombre.equals("datos.json")) return true;
        return nombre.startsWith(PREFIJO_IMAGENES)
                && nombre.substring(PREFIJO_IMAGENES.length()).matches("[\\w\\-]+\\.(png|jpg|jpeg|gif|bmp)");
    }
}