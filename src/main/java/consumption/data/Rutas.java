package consumption.data;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public class Rutas {

    private static final String AISLADO = System.getProperty("maw.generos.aislado");
    public static final Path BASE = resolverBase();

    private static Path resolverBase() {
        if (AISLADO == null) return Path.of(System.getProperty("user.home"), "consumption-personal");
        try {
            Path temporal = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
            Path ruta = Path.of(AISLADO).toAbsolutePath().normalize();
            Path real = ruta.toRealPath();
            if (!ruta.equals(real) || !temporal.equals(real.getParent())
                    || !real.getFileName().toString().startsWith("maw-32c-"))
                throw new IOException("Directorio de ensayo fuera del temporal autorizado.");
            Path marca = real.resolve(".maw-32c-sintetico");
            if (!Files.isRegularFile(marca, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || !Files.readString(marca).equals("MAW32C_SYNTHETIC_ONLY\n"))
                throw new IOException("Falta la marca del entorno sintetico.");
            try (var archivos = Files.walk(real)) {
                if (archivos.anyMatch(Files::isSymbolicLink))
                    throw new IOException("El entorno de ensayo contiene enlaces simbolicos.");
            }
            return real;
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Activacion aislada rechazada. No se usara el perfil personal.", ex);
        }
    }

    public static boolean generosAislados() { return AISLADO != null; }
    public static boolean generosHabilitados() {
        return generosAislados() || Boolean.parseBoolean(System.getProperty("maw.generos.habitual", "true"));
    }
    static void exigirGenerosHabilitados() {
        if (!generosHabilitados()) throw new IllegalStateException("Formato 2 deshabilitado.");
    }
    static void exigirOrigenEnsayo(Path archivo) throws IOException {
        exigirGenerosHabilitados();
        if (!generosAislados()) return;
        Path ruta = archivo.toAbsolutePath().normalize();
        Path real = ruta.toRealPath();
        if (!ruta.equals(real) || !real.startsWith(BASE) || !Files.isRegularFile(real))
            throw new IOException("El respaldo de ensayo debe estar en la carpeta sintetica, sin enlaces.");
    }
    public static final Path DATOS = BASE.resolve("datos.json");
    public static final Path IMAGENES = BASE.resolve("imagenes");
    public static final Path PREFERENCIAS = BASE.resolve("preferencias.json");
    public static final Path RESPALDOS = BASE.resolve("respaldos");
    public static final Path PAPELERA = BASE.resolve("papelera.json");

    private Rutas() {}
}