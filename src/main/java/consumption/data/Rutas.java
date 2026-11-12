package consumption.data;

import java.nio.file.Path;

public class Rutas {

    public static final Path BASE = Path.of(System.getProperty("user.home"), "consumption-personal");
    public static final Path DATOS = BASE.resolve("datos.json");
    public static final Path IMAGENES = BASE.resolve("imagenes");

    private Rutas() {}
}