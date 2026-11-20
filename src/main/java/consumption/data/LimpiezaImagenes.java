package consumption.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class LimpiezaImagenes {
    private static final Path CUARENTENA = Rutas.BASE.resolve("imagenes-cuarentena");
    private static final String PATRON =
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|jpeg|gif|bmp)";

    private LimpiezaImagenes() { }

    public record Candidata(String nombre, long bytes) { }
    public record Informe(List<Candidata> candidatas, long bytes) {
        public Informe {
            candidatas = List.copyOf(candidatas);
        }
    }

    public static List<String> referenciasAusentes() {
        try {
            Set<String> usadas = new HashSet<>();
            leerReferencias(Rutas.DATOS, usadas, true);
            leerReferencias(Rutas.PAPELERA, usadas, false);
            if (Files.exists(Rutas.IMAGENES, LinkOption.NOFOLLOW_LINKS)
                    && (!Files.isDirectory(Rutas.IMAGENES, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(Rutas.IMAGENES))) {
                throw new IOException("Directorio de imagenes invalido");
            }
            List<String> ausentes = new ArrayList<>();
            for (String nombre : usadas) {
                Path archivo = Rutas.IMAGENES.resolve(nombre);
                if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(archivo)) ausentes.add(nombre);
            }
            ausentes.sort(String::compareTo);
            return List.copyOf(ausentes);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Diagnostico cancelado sin modificar imagenes: "
                    + ex.getMessage(), ex);
        }
    }

    public static Path carpetaCuarentena() {
        return CUARENTENA;
    }

    public static Informe analizar() {
        try {
            Set<String> usadas = new HashSet<>();
            leerReferencias(Rutas.DATOS, usadas, true);
             leerReferencias(Rutas.PAPELERA, usadas, false);

            List<Candidata> candidatas = new ArrayList<>();
            if (!Files.exists(Rutas.IMAGENES, LinkOption.NOFOLLOW_LINKS)) {
                return new Informe(candidatas, 0);
            }
            if (!Files.isDirectory(Rutas.IMAGENES, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(Rutas.IMAGENES)) {
                throw new IOException("La carpeta de imagenes no es un directorio normal.");
            }
            try (DirectoryStream<Path> archivos = Files.newDirectoryStream(Rutas.IMAGENES)) {
                for (Path archivo : archivos) {
                    String nombre = archivo.getFileName().toString();
                    if (!nombre.matches(PATRON)) continue;
                    if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)
                            || Files.isSymbolicLink(archivo)) continue;
                    if (!usadas.contains(nombre)) {
                        candidatas.add(new Candidata(nombre, Files.size(archivo)));
                    }
                }
            }
            candidatas.sort(Comparator.comparing(Candidata::nombre));
            long bytes = 0;
            for (Candidata candidata : candidatas) bytes = Math.addExact(bytes, candidata.bytes());
            return new Informe(candidatas, bytes);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Analisis cancelado; no se movio ninguna imagen: "
                    + ex.getMessage(), ex);
        }
    }

    private static void leerReferencias(Path ruta, Set<String> usadas, boolean obligatoria)
            throws IOException {
        if (!Files.exists(ruta, LinkOption.NOFOLLOW_LINKS)) {
            if (obligatoria) throw new IOException("No existe " + ruta.getFileName());
            return;
        }
        if (!Files.isRegularFile(ruta, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(ruta)) {
            throw new IOException("Archivo de datos no valido: " + ruta.getFileName());
        }
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(lector);
            if (!raiz.isJsonObject()) throw new IOException("Raiz JSON invalida: " + ruta);
            JsonObject objeto = raiz.getAsJsonObject();
            if (ruta.equals(Rutas.DATOS)) {
                for (String categoria : List.of("juegos", "libros", "peliculas", "series")) {
                    JsonElement lista = objeto.get(categoria);
                    if (lista != null && !lista.isJsonArray()) {
                        throw new IOException("Categoria invalida: " + categoria);
                    }
                }
            } else {
                JsonElement entradas = objeto.get("entradas");
                if (entradas == null || !entradas.isJsonArray()) {
                    throw new IOException("Papelera sin entradas validas");
                }
            }
            recolectar(raiz, usadas);
        } catch (RuntimeException ex) {
            throw new IOException("JSON invalido en " + ruta.getFileName(), ex);
        }
    }

    private static void recolectar(JsonElement elemento, Set<String> usadas) throws IOException {
        if (elemento == null || elemento.isJsonNull()) return;
        if (elemento.isJsonArray()) {
            for (JsonElement hijo : elemento.getAsJsonArray()) recolectar(hijo, usadas);
        } else if (elemento.isJsonObject()) {
            for (var entrada : elemento.getAsJsonObject().entrySet()) {
                if (entrada.getKey().equals("rutaImagen") && !entrada.getValue().isJsonNull()) {
                    JsonElement valor = entrada.getValue();
                    if (!valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isString()) {
                        throw new IOException("Referencia de imagen invalida en JSON");
                    }
                    String nombre = valor.getAsString();
                    if (!nombre.matches(PATRON)) {
                        throw new IOException("Referencia de imagen no estandar: " + nombre);
                    }
                    usadas.add(nombre);
                } else {
                    recolectar(entrada.getValue(), usadas);
                }
            }
        }
    }

    public static int ponerEnCuarentena(Informe aprobado) {
        final int[] movidas = new int[1];
        BarreraEscritura.escritura(() -> movidas[0] = ponerEnCuarentenaProtegido(aprobado));
        return movidas[0];
    }

    private static int ponerEnCuarentenaProtegido(Informe aprobado) {
        if (aprobado == null) throw new IllegalArgumentException("Falta el informe aprobado.");
        Informe actual = analizar();
        if (!actual.equals(aprobado)) {
            throw new IllegalStateException("Las imagenes o sus referencias cambiaron. "
                    + "Vuelve a analizar antes de confirmar.");
        }
        if (actual.candidatas().isEmpty()) return 0;

        List<Path[]> movidos = new ArrayList<>();
        try {
            Files.createDirectories(CUARENTENA);
            if (Files.isSymbolicLink(CUARENTENA)
                    || !Files.isDirectory(CUARENTENA, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("La cuarentena no es un directorio normal.");
            }
            String lote = UUID.randomUUID().toString();
            Path destinoLote = CUARENTENA.resolve(lote);
            Files.createDirectory(destinoLote);
            for (Candidata candidata : actual.candidatas()) {
                Path origen = Rutas.IMAGENES.resolve(candidata.nombre());
                if (!Files.isRegularFile(origen, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(origen)
                        || Files.size(origen) != candidata.bytes()) {
                    throw new IOException("Cambio detectado en " + candidata.nombre());
                }
                Path destino = destinoLote.resolve(candidata.nombre());
                Files.move(origen, destino);
                movidos.add(new Path[]{origen, destino});
            }
            GestorImagenes.limpiarCache();
            return movidos.size();
        } catch (IOException | RuntimeException ex) {
            for (int i = movidos.size() - 1; i >= 0; i--) {
                Path[] par = movidos.get(i);
                try {
                    if (!Files.exists(par[0], LinkOption.NOFOLLOW_LINKS)) {
                        Files.move(par[1], par[0]);
                    }
                } catch (IOException fallo) {
                    ex.addSuppressed(fallo);
                }
            }
            throw new IllegalStateException("No se completo la cuarentena. "
                    + "Revisa imagenes/ e imagenes-cuarentena/: " + ex.getMessage(), ex);
        }
    }
}
