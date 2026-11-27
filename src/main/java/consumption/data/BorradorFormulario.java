package consumption.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
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
        public transient java.util.List<consumption.model.ReferenciaGenero> generos;
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
            com.google.gson.JsonObject raiz = com.google.gson.JsonParser.parseReader(lector).getAsJsonObject();
            Datos datos = GSON.fromJson(raiz, Datos.class);
            if (datos == null || (datos.version != VERSION && datos.version != FormatoGeneros.VERSION)
                    || !tipo.equals(datos.tipo) || datos.campos == null) {
                throw new IllegalStateException("Borrador incompatible");
            }
            if (datos.version == FormatoGeneros.VERSION) {
                if (datos.campos.keySet().stream().anyMatch(k -> k.equals("genero") || k.startsWith("GENERO#")))
                    throw new IllegalStateException("Borrador con genero legado y referencias simultaneos.");
                datos.generos = FormatoGeneros.leerReferencias(raiz);
            } else if (raiz.has(FormatoGeneros.REFERENCIAS)) {
                throw new IllegalStateException("Borrador legado con referencias de genero.");
            }
            return Optional.of(datos);
        } catch (IOException | JsonParseException | IllegalStateException ex) {
            throw new IllegalStateException(
                    "No se pudo leer " + archivo.getFileName() + ": " + ex.getMessage(), ex);
        }
    }

    public static RecuperacionGeneros.Plan prepararGeneros(Datos datos, consumption.model.BibliotecaGeneros biblioteca,
                                                           String generoLegado) {
        consumption.model.AmbitoGenero ambito = switch (datos.tipo) {
            case "JUEGO" -> consumption.model.AmbitoGenero.JUEGOS;
            case "LIBRO" -> consumption.model.AmbitoGenero.LIBROS;
            case "PELICULA", "SERIE" -> consumption.model.AmbitoGenero.AUDIOVISUAL;
            default -> throw new IllegalArgumentException("Tipo de borrador desconocido.");
        };
        return RecuperacionGeneros.preparar(biblioteca, ambito,
                datos.generos == null ? java.util.List.of() : datos.generos,
                generoLegado, datos.generos != null);
    }

    public static void guardar(String tipo, Map<String, String> campos) { guardar(tipo, campos, null); }
    public static void guardar(String tipo, Map<String, String> campos,
                               java.util.List<consumption.model.ReferenciaGenero> generos) {
        Path archivo = ruta(tipo);
        if (generos != null) Rutas.exigirGenerosHabilitados();
        Datos datos = new Datos();
        datos.tipo = tipo;
        datos.campos = new LinkedHashMap<>(campos);
        if (generos != null) {
            datos.version = FormatoGeneros.VERSION;
            datos.generos = java.util.List.copyOf(generos);
            datos.campos.keySet().removeIf(k -> k.equals("genero") || k.startsWith("GENERO#"));
        }
        BarreraEscritura.escritura(() -> {
            Path temporal = null;
            try {
                Files.createDirectories(archivo.getParent());
                if (generos == null && leer(tipo).filter(d -> d.version == FormatoGeneros.VERSION).isPresent())
                    throw new IllegalStateException("No se puede sobrescribir un borrador de generos desde el formulario legado.");
                temporal = Files.createTempFile(archivo.getParent(), "borrador-", ".tmp");
                com.google.gson.JsonObject raiz = GSON.toJsonTree(datos).getAsJsonObject();
                if (datos.generos != null) raiz.add(FormatoGeneros.REFERENCIAS, FormatoGeneros.escribirReferencias(datos.generos));
                byte[] bytes = GSON.toJson(raiz).getBytes(StandardCharsets.UTF_8);
                Files.write(temporal, bytes);
                if (generos != null) {
                    try (var canal = java.nio.channels.FileChannel.open(temporal, java.nio.file.StandardOpenOption.WRITE)) {
                        canal.force(true);
                    }
                    if (!java.util.Arrays.equals(bytes, Files.readAllBytes(temporal)))
                        throw new IOException("Borrador temporal incoherente.");
                }
                try {
                    Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    if (generos != null) throw ex;
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo guardar el borrador: " + ex.getMessage(), ex);
            } finally {
                if (temporal != null) try { Files.deleteIfExists(temporal); }
                catch (IOException ex) { System.err.println("Temporal de borrador pendiente: " + temporal); }
            }
        });
    }

    public static void borrar(String tipo) {
        BarreraEscritura.escritura(() -> {
            try {
                Files.deleteIfExists(ruta(tipo));
        } catch (IOException ex) {
                throw new IllegalStateException("No se pudo borrar el borrador: " + ex.getMessage(), ex);
            }
        });
    }
}
