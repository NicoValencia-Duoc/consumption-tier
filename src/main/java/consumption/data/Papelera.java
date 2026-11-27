package consumption.data;

import com.google.gson.*;
import consumption.model.*;

import java.io.IOException;
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
    private int versionArchivo = 1;
    private byte[] instantanea;

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

    public ItemConsumption recuperarConGeneros(Entrada entrada, List<ItemConsumption> guardados,
                                               RepositorioJson repositorio, RecuperacionGeneros.Plan plan, Set<Integer> autorizados) throws Exception {
        if (!entradas.contains(entrada)) throw new IllegalArgumentException("Entrada de papelera inexistente.");
        ItemConsumption copia = repositorio.copiarParaRecuperacion(entrada.item());
        if (copia.getNumero() <= 0 || guardados.stream().anyMatch(i -> i.getNumero() == copia.getNumero()))
            copia.setNumero(Numeracion.siguiente(guardados));
        List<ItemConsumption> nuevos = new ArrayList<>(guardados);
        nuevos.add(copia);
        try {
            DiarioRestauracion.ejecutar(() -> {
                repositorio.guardarRecuperacion(nuevos, copia, plan, autorizados);
                retirar(Set.of(entrada.id()));
            });
        } catch (Exception ex) {
            repositorio.invalidarCargaGeneros();
            try { recargar(); } catch (RuntimeException recarga) { ex.addSuppressed(recarga); }
            throw ex;
        }
        guardados.add(copia);
        return copia;
    }

    public record RecuperacionLote(Entrada entrada, RecuperacionGeneros.Plan plan, Set<Integer> autorizados) {
        public RecuperacionLote { autorizados = Set.copyOf(autorizados); }
    }

    public void recuperarLoteConGeneros(List<RecuperacionLote> lote, List<ItemConsumption> guardados,
                                        RepositorioJson repositorio) throws Exception {
        if (lote.isEmpty()) return;
        Set<String> ids = new HashSet<>();
        List<ItemConsumption> nuevos = new ArrayList<>(guardados);
        List<ItemConsumption> copias = new ArrayList<>();
        List<RepositorioJson.RecuperacionSolicitada> solicitudes = new ArrayList<>();
        Set<Integer> numeros = new HashSet<>();
        guardados.forEach(i -> numeros.add(i.getNumero()));
        for (var solicitud : lote) {
            var entrada = solicitud.entrada();
            if (!entradas.contains(entrada) || !ids.add(entrada.id()))
                throw new IllegalArgumentException("Entrada de papelera inexistente o duplicada.");
            ItemConsumption copia = repositorio.copiarParaRecuperacion(entrada.item());
            if (copia.getNumero() <= 0 || numeros.contains(copia.getNumero()))
                copia.setNumero(Numeracion.siguiente(nuevos));
            numeros.add(copia.getNumero());
            nuevos.add(copia); copias.add(copia);
            solicitudes.add(new RepositorioJson.RecuperacionSolicitada(copia, solicitud.plan(), solicitud.autorizados()));
        }
        try {
            DiarioRestauracion.ejecutar(() -> {
                repositorio.guardarRecuperaciones(nuevos, solicitudes);
                retirar(ids);
            });
        } catch (Exception ex) {
            repositorio.invalidarCargaGeneros();
            try { recargar(); } catch (RuntimeException recarga) { ex.addSuppressed(recarga); }
            throw ex;
        }
        guardados.addAll(copias);
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
        versionArchivo = 1;
        instantanea = null;
        if (!Files.exists(Rutas.PAPELERA)) return;
        try {
            byte[] bytes = Files.readAllBytes(Rutas.PAPELERA);
            String texto = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            JsonElement raiz = JsonParser.parseString(texto);
            if (!raiz.isJsonObject()) throw new JsonParseException("Raiz invalida");
            JsonObject objeto = raiz.getAsJsonObject();
            if (!objeto.has("version")) throw new JsonParseException("Falta version de papelera");
            int version = objeto.get("version").getAsBigDecimal().intValueExact();
            if (version != 1 && version != 2) throw new JsonParseException("Version de papelera incompatible");
            JsonArray lista = objeto.getAsJsonArray("entradas");
            if (lista == null) throw new JsonParseException("Falta entradas");
            List<Entrada> leidas = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonElement elemento : lista) {
                JsonObject e = elemento.getAsJsonObject();
                String id = e.get("id").getAsString();
                LocalDateTime fecha = LocalDateTime.parse(e.get("eliminadaEn").getAsString());
                String tipo = e.get("tipo").getAsString();
                Class<? extends ItemConsumption> clase = claseDe(tipo);
                JsonObject fila = e.getAsJsonObject("item");
                ItemConsumption item = gson.fromJson(fila, clase);
                if (fila.has(FormatoGeneros.REFERENCIAS)) {
                    if (version != 2 || fila.has("genero")) throw new JsonParseException("Formato de genero ambiguo en papelera.");
                    item.setGeneros(FormatoGeneros.leerReferencias(fila));
                }
                if (id.isBlank() || !ids.add(id) || item == null || item.getTitulo() == null
                        || item.getTitulo().isBlank() || item.getEstado() == null)
                    throw new JsonParseException("Entrada incompleta");
                leidas.add(new Entrada(id, fecha, tipo, item));
            }
            entradas.addAll(leidas);
            versionArchivo = version;
            instantanea = bytes;
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
        int version = versionArchivo == 2 || lista.stream().anyMatch(e -> e.item().tieneGenerosMigrados()) ? 2 : 1;
        raiz.addProperty("version", version);
        JsonArray array = new JsonArray();
        for (Entrada e : lista) {
            JsonObject registro = new JsonObject();
            registro.addProperty("id", e.id());
            registro.addProperty("eliminadaEn", e.eliminadaEn().toString());
            registro.addProperty("tipo", e.tipo());
            JsonObject fila = gson.toJsonTree(e.item()).getAsJsonObject();
            if (e.item().tieneGenerosMigrados()) {
                fila.remove("genero");
                fila.add(FormatoGeneros.REFERENCIAS, FormatoGeneros.escribirReferencias(e.item().getGeneros()));
            }
            registro.add("item", fila);
            array.add(registro);
        }
        raiz.add("entradas", array);

        Path temporal = null;
        try {
            Files.createDirectories(Rutas.BASE);
            boolean protegido = Rutas.generosHabilitados() || version == 2;            if (protegido) comprobarInstantanea();
            temporal = Files.createTempFile(Rutas.BASE, "papelera-", ".tmp");
            byte[] bytes = gson.toJson(raiz).getBytes(StandardCharsets.UTF_8);
            try (var canal = java.nio.channels.FileChannel.open(temporal, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) canal.write(buffer);
                if (protegido) canal.force(true);
            }
            if (protegido) {
                if (!Arrays.equals(bytes, Files.readAllBytes(temporal))) throw new IOException("Papelera temporal incoherente.");
                comprobarInstantanea();
            }
            try {
                Files.move(temporal, Rutas.PAPELERA,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                if (protegido) throw ex;
                Files.move(temporal, Rutas.PAPELERA,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            instantanea = bytes;
            versionArchivo = version;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar papelera.json", ex);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException ignorada) { }
            }
        }
    }
    private void comprobarInstantanea() throws IOException {
        if (!Arrays.equals(instantanea, Files.exists(Rutas.PAPELERA) ? Files.readAllBytes(Rutas.PAPELERA) : null))
            throw new IOException("La papelera cambio desde la carga. Recarga antes de guardar.");
    }
}
