package consumption.data;

import com.google.gson.*;
import consumption.model.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class Papelera {
    public static final int DIAS_RETENCION = 15;

    public record Entrada(String id, LocalDateTime eliminadaEn,
                          String tipo, ItemConsumption item) {
        @Override public String toString() {
            return item.getTitulo() + " [" + tipo + "] · "
                    + eliminadaEn.toLocalDate();
        }
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new AdaptadorFecha().nullSafe())
            .setPrettyPrinting().create();
    private final List<Entrada> entradas = new ArrayList<>();

    public Papelera() {
        cargar();
    }

    public void recargar() {
        entradas.clear();
        cargar();
    }

    public List<Entrada> listar() {
        return List.copyOf(entradas);
    }

    public void enviar(List<? extends ItemConsumption> items) {
        if (items.isEmpty()) return;
        List<Entrada> nuevas = new ArrayList<>(entradas);
        LocalDateTime ahora = LocalDateTime.now();
        for (ItemConsumption item : items) {
            nuevas.add(new Entrada(UUID.randomUUID().toString(),
                    ahora, item.getTipo(), item));
        }
        guardar(nuevas);
        entradas.clear();
        entradas.addAll(nuevas);
    }

    public void retirar(Collection<String> ids) {
        if (ids.isEmpty()) return;
        List<Entrada> nuevas = new ArrayList<>(entradas);
        nuevas.removeIf(e -> ids.contains(e.id()));
        guardar(nuevas);
        entradas.clear();
        entradas.addAll(nuevas);
    }

    public void depurarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Entrada> nuevas = entradas.stream()
                .filter(e -> e.eliminadaEn().plusDays(DIAS_RETENCION).isAfter(ahora))
                .toList();
        if (nuevas.size() != entradas.size()) {
            guardar(nuevas);
            entradas.clear();
            entradas.addAll(nuevas);
        }
    }

    private void cargar() {
        if (!Files.exists(Rutas.PAPELERA)) return;
        try (Reader reader = Files.newBufferedReader(Rutas.PAPELERA,
                StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(reader);
            if (!raiz.isJsonObject()) throw new JsonParseException("Raiz invalida");
            JsonObject objeto = raiz.getAsJsonObject();
            if (!objeto.has("version") || objeto.get("version").getAsInt() != 1)
                throw new JsonParseException("Version de papelera incompatible");
            JsonArray lista = objeto.getAsJsonArray("entradas");
            if (lista == null) throw new JsonParseException("Falta entradas");
            List<Entrada> leidas = new ArrayList<>();
            for (JsonElement elemento : lista) {
                JsonObject e = elemento.getAsJsonObject();
                String id = e.get("id").getAsString();
                LocalDateTime fecha = LocalDateTime.parse(e.get("eliminadaEn").getAsString());
                String tipo = e.get("tipo").getAsString();
                Class<? extends ItemConsumption> clase = claseDe(tipo);
                ItemConsumption item = gson.fromJson(e.get("item"), clase);
                if (id.isBlank() || item == null || item.getTitulo() == null
                        || item.getTitulo().isBlank() || item.getEstado() == null)
                    throw new JsonParseException("Entrada incompleta");
                leidas.add(new Entrada(id, fecha, tipo, item));
            }
            entradas.addAll(leidas);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se puede leer papelera.json. Conserva ese archivo; "
                            + "no se realizara ninguna eliminacion. Detalle: "
                            + ex.getMessage(), ex);
        }
    }

    private static Class<? extends ItemConsumption> claseDe(String tipo) {
        return switch (tipo) {
            case "JUEGOS" -> Juego.class;
            case "LIBROS" -> Libro.class;
            case "PELICULAS" -> Pelicula.class;
            case "SERIES" -> Serie.class;
            default -> throw new JsonParseException("Tipo desconocido: " + tipo);
        };
    }

    private void guardar(List<Entrada> lista) {
        BarreraEscritura.escritura(() -> guardarInterno(lista));
    }

    private void guardarInterno(List<Entrada> lista) {
        JsonObject raiz = new JsonObject();
        raiz.addProperty("version", 1);
        JsonArray array = new JsonArray();
        for (Entrada e : lista) {
            JsonObject registro = new JsonObject();
            registro.addProperty("id", e.id());
            registro.addProperty("eliminadaEn", e.eliminadaEn().toString());
            registro.addProperty("tipo", e.tipo());
            registro.add("item", gson.toJsonTree(e.item()));
            array.add(registro);
        }
        raiz.add("entradas", array);

        Path temporal = null;
        try {
            Files.createDirectories(Rutas.BASE);
            temporal = Files.createTempFile(Rutas.BASE, "papelera-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporal,
                    StandardCharsets.UTF_8)) {
                gson.toJson(raiz, writer);
            }
            try {
                Files.move(temporal, Rutas.PAPELERA,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, Rutas.PAPELERA,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar papelera.json", ex);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException ignorada) { }
            }
        }
    }
}
