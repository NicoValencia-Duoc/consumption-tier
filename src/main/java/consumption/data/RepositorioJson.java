package consumption.data;

import consumption.model.*;
import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepositorioJson {
    private static final Path ARCHIVO = Rutas.DATOS;
    private static final int VERSION_ACTUAL = 1;
    private static final List<String> CATEGORIAS =
            List.of("juegos", "libros", "peliculas", "series");

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new AdaptadorFecha().nullSafe())
            .setPrettyPrinting()
            .create();

    private final JsonObject conservados = new JsonObject();
    private final List<String> avisos = new ArrayList<>();
    private boolean cargaCompletada = false;

    public static final class Rechazado {
        private final String categoria;
        private final JsonElement elemento;
        private final String motivo;

        private Rechazado(String categoria, JsonElement elemento, String motivo) {
            this.categoria = categoria;
            this.elemento = elemento;
            this.motivo = motivo;
        }

        public String getCategoria() { return categoria; }
        public String getMotivo() { return motivo; }

        public String getResumen() {
            if (elemento.isJsonObject() && elemento.getAsJsonObject().has("titulo")) {
                JsonElement titulo = elemento.getAsJsonObject().get("titulo");
                if (titulo.isJsonPrimitive()) return titulo.getAsString();
            }
            return motivo.contains(":") ? motivo.substring(0, motivo.indexOf(':')) : motivo;
        }

        public String getTexto() {
            return new GsonBuilder().setPrettyPrinting().create().toJson(elemento);
        }
    }

    private final List<Rechazado> detalleRechazados = new ArrayList<>();

    public int cantidadRechazados() {
        return conservados.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray())
                .mapToInt(e -> e.getValue().getAsJsonArray().size()).sum();
    }

    public boolean tieneRechazados() {
        return conservados.entrySet().stream().anyMatch(e -> e.getValue().isJsonArray()
                && e.getValue().getAsJsonArray().size() > 0);
    }

    public String huellaRechazados() {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (String categoria : CATEGORIAS) {
                JsonArray arr = conservados.getAsJsonArray(categoria);
                if (arr == null || arr.isEmpty()) continue;
                digest.update(categoria.getBytes(StandardCharsets.UTF_8));
                digest.update(arr.toString().getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Path exportarRechazados(Path destino) {
        if (!cargaCompletada || !tieneRechazados())
            throw new IllegalStateException("No hay registros rechazados disponibles para exportar.");
        JsonObject exportacion = new JsonObject();
        exportacion.addProperty("versionExportacion", 1);
        for (String categoria : CATEGORIAS) {
            JsonArray arr = conservados.getAsJsonArray(categoria);
            exportacion.add(categoria, arr == null ? new JsonArray() : arr.deepCopy());
        }
        Path temporal = null;
        try {
            Path padre = destino.toAbsolutePath().getParent();
            Files.createDirectories(padre);
            if (Files.exists(destino))
                throw new java.nio.file.FileAlreadyExistsException(destino.toString());
            temporal = Files.createTempFile(padre, "rechazados-", ".tmp");
            Files.writeString(temporal, gson.toJson(exportacion), StandardCharsets.UTF_8);
            try { Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporal, destino); }
            return destino;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo exportar los registros rechazados.", ex);
        } finally {
            if (temporal != null) try { Files.deleteIfExists(temporal); }
            catch (IOException ex) { System.err.println("Temporal pendiente: " + temporal); }
        }
    }

    public List<Rechazado> getRechazados() {
        return List.copyOf(detalleRechazados);
    }

    public ItemConsumption interpretar(Rechazado rechazado, String texto) {
        Class<? extends ItemConsumption> tipo = switch (rechazado.categoria) {
            case "juegos" -> Juego.class;
            case "libros" -> Libro.class;
            case "peliculas" -> Pelicula.class;
            case "series" -> Serie.class;
            default -> throw new IllegalArgumentException("Categoria desconocida: " + rechazado.categoria);
        };
        JsonElement elemento;
        try {
            elemento = JsonParser.parseString(texto);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("JSON mal formado: " + mensaje(ex));
        }
        if (!elemento.isJsonObject()) throw new IllegalArgumentException("No es un objeto JSON");
        try {
            ItemConsumption item = gson.fromJson(elemento, tipo);
            validar(item);
            return item;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(mensaje(ex));
        }
    }

    public void quitarRechazado(Rechazado rechazado) {
        JsonArray lista = conservados.getAsJsonArray(rechazado.categoria);
        if (lista == null || !lista.remove(rechazado.elemento)) {
            throw new IllegalStateException("El registro ya no esta pendiente.");
        }
        detalleRechazados.remove(rechazado);
    }

    public void reponerRechazado(Rechazado rechazado) {
        JsonArray lista = conservados.getAsJsonArray(rechazado.categoria);
        if (lista == null) {
            lista = new JsonArray();
            conservados.add(rechazado.categoria, lista);
        }
        lista.add(rechazado.elemento);
        if (!detalleRechazados.contains(rechazado)) detalleRechazados.add(rechazado);
    }

    public Path guardarCopiaDescartado(Rechazado rechazado) {
        try {
            Path carpeta = Rutas.BASE.resolve("descartados");
            Files.createDirectories(carpeta);
            String sello = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS"));
            Path destino = carpeta.resolve("descartado-" + rechazado.categoria + "-" + sello + ".json");
            Files.writeString(destino, rechazado.getTexto(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            return destino;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la copia del registro: " + ex.getMessage(), ex);
        }
    }

    public List<String> getAvisosCarga() {
        return List.copyOf(avisos);
    }

    public List<ItemConsumption> cargar() {
        avisos.clear();
        detalleRechazados.clear();
        conservados.entrySet().clear();
        cargaCompletada = false;
        if (!Files.exists(ARCHIVO)) {
            cargaCompletada = true;
            return new ArrayList<>();
        }

        try (Reader lector = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(lector);
            if (!raiz.isJsonObject()) {
                throw new JsonParseException("La raiz no es un objeto JSON.");
            }
            JsonObject objeto = raiz.getAsJsonObject();
            int version = 0;
            if (objeto.has("versionFormato")) {
                JsonElement valor = objeto.get("versionFormato");
                if (!valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isNumber()) {
                    throw new JsonParseException("versionFormato no es numerico.");
                }
                version = valor.getAsInt();
            }
            if (version < 0 || version > VERSION_ACTUAL) {
                throw new JsonParseException("Version incompatible: " + version);
            }

            List<ItemConsumption> todos = new ArrayList<>();
            leerCategoria(objeto, "juegos", Juego.class, todos);
            leerCategoria(objeto, "libros", Libro.class, todos);
            leerCategoria(objeto, "peliculas", Pelicula.class, todos);
            leerCategoria(objeto, "series", Serie.class, todos);
            cargaCompletada = true;
            return todos;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new IllegalStateException(
                    "No se pudo cargar datos.json. No se ha sobrescrito. "
                            + "Conserva el archivo y sus respaldos.", e);
        }
    }

    private <T extends ItemConsumption> void leerCategoria(
            JsonObject raiz, String nombre, Class<T> tipo, List<ItemConsumption> destino) {
        JsonElement valor = raiz.get(nombre);
        if (valor == null) return;
        if (!valor.isJsonArray()) {
            throw new JsonParseException(nombre + " no es una lista JSON.");
        }
        JsonArray rechazados = new JsonArray();
        int posicion = 0;
        for (JsonElement elemento : valor.getAsJsonArray()) {
            posicion++;
            try {
                if (!elemento.isJsonObject()) {
                    throw new IllegalArgumentException("No es un objeto JSON");
                }
                T item = gson.fromJson(elemento, tipo);
                validar(item);
                destino.add(item);
            } catch (RuntimeException ex) {
                JsonElement copia = elemento.deepCopy();
                rechazados.add(copia);
                String motivo = mensaje(ex);
                avisos.add(nombre + " [registro " + posicion + "]: " + motivo);
                detalleRechazados.add(new Rechazado(nombre, copia, "registro " + posicion + ": " + motivo));
            }
        }
        conservados.add(nombre, rechazados);
    }

    private String mensaje(RuntimeException ex) {
        String detalle = ex.getMessage();
        return detalle == null || detalle.isBlank()
                ? ex.getClass().getSimpleName() : detalle;
    }

    private void validar(ItemConsumption i) {
        if (i == null) throw new IllegalArgumentException("Registro vacio");
        i.setTitulo(i.getTitulo());
        i.setEstado(i.getEstado());
        i.setNota(i.getNota());
        i.setPorcentaje(i.getPorcentaje());
        i.setComentario(i.getComentario());
        i.setConQuien(i.getConQuien());
        if (i.getVecesConsumido() < 0 || i.getVecesConsumido() > ItemConsumption.MAX_VECES) {
            throw new IllegalArgumentException("Contador fuera de rango");
        }
        for (var criterio : i.getCriterios().entrySet()) {
            String nombre = criterio.getKey();
            Double valor = criterio.getValue();
            if (!i.getNombresCriterios().contains(nombre) || valor == null
                    || !Double.isFinite(valor) || valor < 0 || valor > 10) {
                throw new IllegalArgumentException("Criterio invalido: " + nombre);
            }
        }
        if (!Double.isFinite(i.getNota()) || !Double.isFinite(i.getPorcentaje())) {
            throw new IllegalArgumentException("Nota o avance no finito");
        }
        if (i instanceof Juego j) {
            j.setPlataforma(j.getPlataforma());
            j.setGenero(j.getGenero());
            j.setDesarrollador(j.getDesarrollador());
            j.setAnio(j.getAnio());
            j.setHorasJugadas(j.getHorasJugadas());
            if (!Double.isFinite(j.getHorasJugadas()))
                throw new IllegalArgumentException("Horas no finitas");
        } else if (i instanceof Libro l) {
            l.setAutor(l.getAutor());
            l.setGenero(l.getGenero());
            l.setPaginas(l.getPaginas());
            l.setAnio(l.getAnio());
        } else if (i instanceof Pelicula p) {
            p.setDirector(p.getDirector());
            p.setGenero(p.getGenero());
            p.setDuracionMin(p.getDuracionMin());
            p.setAnio(p.getAnio());
        } else if (i instanceof Serie s) {
            s.setCreador(s.getCreador());
            s.setGenero(s.getGenero());
            s.setAnio(s.getAnio());
            s.setTemporadas(s.getTemporadas());
            s.setEpisodios(s.getEpisodios());
            s.setOrigen(s.getOrigen());
        }
        i.validarCoherencia();
    }

    public void guardar(List<ItemConsumption> items) {
        BarreraEscritura.escritura(() -> guardarInterno(items));
    }

    private void guardarInterno(List<ItemConsumption> items) {
        if (!cargaCompletada) {
            throw new IllegalStateException(
                    "Primero debe completarse la carga antes de guardar.");
        }
        JsonObject datos = new JsonObject();
        datos.addProperty("versionFormato", VERSION_ACTUAL);
        for (String categoria : CATEGORIAS) {
            JsonArray lista = new JsonArray();
            for (ItemConsumption i : items) {
                if (pertenece(i, categoria)) lista.add(gson.toJsonTree(i));
            }
            JsonArray pendientes = conservados.getAsJsonArray(categoria);
            if (pendientes != null) {
                for (JsonElement elemento : pendientes) lista.add(elemento.deepCopy());
            }
            datos.add(categoria, lista);
        }

        Path temporal = null;
        try {
            Files.createDirectories(ARCHIVO.getParent());
            temporal = Files.createTempFile(ARCHIVO.getParent(), "datos-", ".tmp");
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                gson.toJson(datos, escritor);
            }
            try {
                Files.move(temporal, ARCHIVO,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar datos.json.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) {
                    System.err.println("No se pudo limpiar temporal: " + temporal);
                }
            }
        }
    }

    private boolean pertenece(ItemConsumption item, String categoria) {
        return switch (categoria) {
            case "juegos" -> item instanceof Juego;
            case "libros" -> item instanceof Libro;
            case "peliculas" -> item instanceof Pelicula;
            case "series" -> item instanceof Serie;
            default -> false;
        };
    }
}