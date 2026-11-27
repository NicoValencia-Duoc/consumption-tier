package consumption.data;

import com.google.gson.*;
import consumption.model.*;
import java.util.*;
import java.util.function.BiPredicate;

public final class FormatoGeneros {
    public static final int VERSION = 2;
    public static final String BIBLIOTECA = "bibliotecaGeneros";
    public static final String REFERENCIAS = "generos";
    private static final List<String> CATEGORIAS = List.of("juegos", "libros", "peliculas", "series");
    private FormatoGeneros() {}

    public static int version(JsonObject raiz) {
        if (!raiz.has("versionFormato")) return 0;
        JsonElement v = raiz.get("versionFormato");
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber())
            throw new JsonParseException("versionFormato no es numerico.");
        try { return v.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException | NumberFormatException ex) {
            throw new JsonParseException("versionFormato no es un entero valido.", ex);
        }
    }

    public static JsonArray escribirBiblioteca(BibliotecaGeneros biblioteca) {
        JsonArray resultado = new JsonArray();
        for (TagGenero tag : biblioteca.registros()) {
            JsonObject fila = escribirReferencia(ReferenciaGenero.de(tag));
            fila.addProperty("ambito", tag.ambito().name());
            fila.addProperty("activo", tag.activo());
            resultado.add(fila);
        }
        return resultado;
    }

    public static BibliotecaGeneros leerBiblioteca(JsonObject raiz) {
        try {
            List<TagGenero> tags = new ArrayList<>();
            for (JsonElement elemento : lista(raiz.get(BIBLIOTECA), BIBLIOTECA)) {
                JsonObject fila = objeto(elemento, BIBLIOTECA);
                ReferenciaGenero referencia = leerReferencia(fila);
                AmbitoGenero ambito = AmbitoGenero.valueOf(cadena(fila, "ambito"));
                JsonElement activo = fila.get("activo");
                if (activo == null || !activo.isJsonPrimitive() || !activo.getAsJsonPrimitive().isBoolean())
                    throw new JsonParseException("Estado de genero invalido.");
                tags.add(new TagGenero(referencia.id(), ambito, referencia.nombre(), activo.getAsBoolean()));
            }
            return new BibliotecaGeneros(tags);
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Biblioteca de generos invalida.", ex);
        }
    }

    public static JsonArray escribirReferencias(List<ReferenciaGenero> referencias) {
        JsonArray resultado = new JsonArray();
        for (ReferenciaGenero referencia : referencias) resultado.add(escribirReferencia(referencia));
        return resultado;
    }

    public static List<ReferenciaGenero> leerReferencias(JsonObject fila) {
        List<ReferenciaGenero> resultado = new ArrayList<>();
        Set<UUID> vistos = new HashSet<>();
        for (JsonElement elemento : lista(fila.get(REFERENCIAS), REFERENCIAS)) {
            ReferenciaGenero referencia = leerReferencia(objeto(elemento, REFERENCIAS));
            if (!vistos.add(referencia.id())) throw new JsonParseException("Referencia de genero duplicada.");
            resultado.add(referencia);
        }
        return List.copyOf(resultado);
    }

    private static JsonObject escribirReferencia(ReferenciaGenero referencia) {
        JsonObject fila = new JsonObject();
        fila.addProperty("id", referencia.id().toString());
        fila.addProperty("nombre", referencia.nombre());
        return fila;
    }

    private static ReferenciaGenero leerReferencia(JsonObject fila) {
        try {
            String textoId = cadena(fila, "id");
            UUID id = UUID.fromString(textoId);
            if (!id.toString().equalsIgnoreCase(textoId)) throw new IllegalArgumentException("UUID invalido.");
            String nombre = cadena(fila, "nombre");
            if (!nombre.equals(NombreGenero.historico(nombre)))
                throw new IllegalArgumentException("Nombre canonico invalido.");
            return new ReferenciaGenero(id, nombre);
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Referencia de genero invalida.", ex);
        }
    }

    public static JsonObject migrar(JsonObject origen, BiPredicate<String, JsonObject> registroValido) {
        Objects.requireNonNull(registroValido);
        int version = version(origen);
        if (version == VERSION) {
            leerBiblioteca(origen);
            return origen.deepCopy();
        }
        if (version < 0 || version > 1 || origen.has(BIBLIOTECA))
            throw new JsonParseException("Formato de datos incompatible con la migracion de generos.");
        JsonObject destino = origen.deepCopy();
        Map<AmbitoGenero, Map<String, TagGenero>> porAmbito = new EnumMap<>(AmbitoGenero.class);
        for (AmbitoGenero ambito : AmbitoGenero.values()) porAmbito.put(ambito, new LinkedHashMap<>());
        Set<UUID> ids = new HashSet<>();
        for (String categoria : CATEGORIAS) {
            if (!destino.has(categoria)) continue;
            AmbitoGenero ambito = AmbitoGenero.deTipo(categoria.toUpperCase(Locale.ROOT));
            for (JsonElement elemento : lista(destino.get(categoria), categoria)) {
                if (!elemento.isJsonObject()) continue;
                JsonObject fila = elemento.getAsJsonObject();
                if (!registroValido.test(categoria, fila.deepCopy())) continue;
                if (fila.has(REFERENCIAS)) throw new JsonParseException("Registro legado con referencias ambiguas.");
                JsonElement genero = fila.get("genero");
                if (genero != null && !genero.isJsonNull()
                        && (!genero.isJsonPrimitive() || !genero.getAsJsonPrimitive().isString()))
                    throw new JsonParseException("Genero historico no textual.");
                String nombre = genero == null || genero.isJsonNull() ? "" : genero.getAsString();
                List<ReferenciaGenero> referencias = List.of();
                if (!NombreGenero.clave(nombre).isEmpty()) {
                    Map<String, TagGenero> indice = porAmbito.get(ambito);
                    TagGenero tag = indice.computeIfAbsent(NombreGenero.clave(nombre), clave -> {
                        UUID id;
                        do { id = UUID.randomUUID(); } while (!ids.add(id));
                        return new TagGenero(id, ambito, nombre, true);
                    });
                    referencias = List.of(ReferenciaGenero.de(tag));
                }
                fila.remove("genero");
                fila.add(REFERENCIAS, escribirReferencias(referencias));
            }
        }
        List<TagGenero> tags = porAmbito.values().stream().flatMap(m -> m.values().stream()).toList();
        BibliotecaGeneros biblioteca = new BibliotecaGeneros(tags);
        destino.addProperty("versionFormato", VERSION);
        destino.add(BIBLIOTECA, escribirBiblioteca(biblioteca));
        return destino;
    }

    public static BibliotecaGeneros reconstruir(Collection<? extends ItemConsumption> items) {
        Map<UUID, TagGenero> tags = new LinkedHashMap<>();
        for (ItemConsumption item : items) {
            if (!item.tieneGenerosMigrados()) throw new IllegalArgumentException("Registro sin migrar.");
            AmbitoGenero ambito = AmbitoGenero.deTipo(item.getTipo());
            for (ReferenciaGenero ref : item.getGeneros()) {
                TagGenero tag = new TagGenero(ref.id(), ambito, ref.nombre(), true);
                TagGenero anterior = tags.putIfAbsent(tag.id(), tag);
                if (anterior != null && !anterior.equals(tag))
                    throw new IllegalArgumentException("Identidad de genero contradictoria.");
            }
        }
        return new BibliotecaGeneros(tags.values());
    }

    private static JsonArray lista(JsonElement elemento, String campo) {
        if (elemento == null || !elemento.isJsonArray()) throw new JsonParseException(campo + " no es una lista JSON.");
        return elemento.getAsJsonArray();
    }
    private static JsonObject objeto(JsonElement elemento, String campo) {
        if (elemento == null || !elemento.isJsonObject()) throw new JsonParseException(campo + " no es un objeto JSON.");
        return elemento.getAsJsonObject();
    }
    private static String cadena(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isString())
            throw new JsonParseException(campo + " no es texto.");
        return valor.getAsString();
    }
}
