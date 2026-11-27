package consumption.model;

import java.util.*;

public final class BibliotecaGeneros {
    private record Clave(AmbitoGenero ambito, String nombre) {}
    public record Alta(BibliotecaGeneros biblioteca, TagGenero tag) {}
    public record Uso(int total, int peliculas, int series) {}

    private final Map<UUID, TagGenero> porId;
    private final Map<Clave, TagGenero> activos;

    public BibliotecaGeneros(Collection<TagGenero> tags) {
        Map<UUID, TagGenero> ids = new LinkedHashMap<>();
        Map<Clave, TagGenero> nombres = new HashMap<>();
        for (TagGenero tag : tags) {
            Objects.requireNonNull(tag);
            if (ids.putIfAbsent(tag.id(), tag) != null)
                throw new IllegalArgumentException("Identidad de genero duplicada: " + tag.id());
            if (tag.activo() && nombres.putIfAbsent(new Clave(tag.ambito(), tag.clave()), tag) != null)
                throw new IllegalArgumentException("Genero activo duplicado en " + tag.ambito() + ": " + tag.nombre());
        }
        porId = Collections.unmodifiableMap(ids);
        activos = Collections.unmodifiableMap(nombres);
    }

    public static BibliotecaGeneros vacia() { return new BibliotecaGeneros(List.of()); }
    public List<TagGenero> registros() { return List.copyOf(porId.values()); }
    public Optional<TagGenero> buscar(UUID id) { return Optional.ofNullable(porId.get(id)); }
    public Optional<TagGenero> buscarActivo(AmbitoGenero ambito, String nombre) {
        return Optional.ofNullable(activos.get(new Clave(Objects.requireNonNull(ambito), NombreGenero.clave(nombre))));
    }
    public List<TagGenero> activos(AmbitoGenero ambito) {
        return porId.values().stream().filter(t -> t.activo() && t.ambito() == ambito)
                .sorted(Comparator.comparing(TagGenero::clave).thenComparing(TagGenero::id)).toList();
    }

    public Alta alta(AmbitoGenero ambito, String nombre) { return alta(ambito, nombre, false); }
    public Alta incorporarHistorico(AmbitoGenero ambito, String nombre) { return alta(ambito, nombre, true); }
    private Alta alta(AmbitoGenero ambito, String nombre, boolean historico) {
        Optional<TagGenero> existente = buscarActivo(ambito, nombre);
        if (existente.isPresent()) return new Alta(this, existente.get());
        String canonico = historico ? NombreGenero.historico(nombre) : NombreGenero.nuevo(nombre);
        UUID id;
        do { id = UUID.randomUUID(); } while (porId.containsKey(id));
        TagGenero tag = new TagGenero(id, ambito, canonico, true);
        var nuevos = new ArrayList<>(registros());
        nuevos.add(tag);
        return new Alta(new BibliotecaGeneros(nuevos), tag);
    }

    public BibliotecaGeneros baja(AmbitoGenero ambito, UUID id) {
        TagGenero tag = exigirActivo(ambito, id);
        var nuevos = new ArrayList<TagGenero>(porId.size());
        for (TagGenero t : porId.values()) nuevos.add(t.id().equals(id) ? tag.darDeBaja() : t);
        return new BibliotecaGeneros(nuevos);
    }

    public TagGenero exigirActivo(AmbitoGenero ambito, UUID id) {
        TagGenero tag = porId.get(id);
        if (tag == null || !tag.activo() || tag.ambito() != ambito)
            throw new IllegalArgumentException("Genero inexistente, eliminado o de otro ambito: " + id);
        return tag;
    }

    public List<TagGenero> resolver(AmbitoGenero ambito, List<ReferenciaGenero> referencias) {
        Set<UUID> vistos = new HashSet<>();
        List<TagGenero> resultado = new ArrayList<>();
        for (ReferenciaGenero referencia : referencias) {
            TagGenero tag = exigirActivo(ambito, referencia.id());
            if (!vistos.add(tag.id()) || !tag.nombre().equals(referencia.nombre()))
                throw new IllegalArgumentException("Referencia de genero incoherente: " + tag.id());
            resultado.add(tag);
        }
        return List.copyOf(resultado);
    }

    public Alta recuperar(AmbitoGenero ambito, UUID eliminado) {
        TagGenero anterior = porId.get(eliminado);
        if (anterior == null || anterior.activo() || anterior.ambito() != ambito)
            throw new IllegalArgumentException("No hay un genero eliminado recuperable en este ambito.");
        return incorporarHistorico(ambito, anterior.nombre());
    }

    public Map<UUID, Uso> usos(AmbitoGenero ambito, Collection<? extends ItemConsumption> items) {
        Map<UUID, int[]> cuentas = new HashMap<>();
        for (ItemConsumption item : items) {
            if (AmbitoGenero.deTipo(item.getTipo()) != ambito) continue;
            for (ReferenciaGenero ref : item.getGeneros()) {
                TagGenero tag = porId.get(ref.id());
                if (tag == null || !tag.activo() || tag.ambito() != ambito) continue;
                int[] c = cuentas.computeIfAbsent(ref.id(), id -> new int[3]);
                c[0]++;
                if (item instanceof Pelicula) c[1]++;
                else if (item instanceof Serie) c[2]++;
            }
        }
        Map<UUID, Uso> resultado = new HashMap<>();
        cuentas.forEach((id, c) -> resultado.put(id, new Uso(c[0], c[1], c[2])));
        return Map.copyOf(resultado);
    }

    public Uso uso(AmbitoGenero ambito, UUID id, Collection<? extends ItemConsumption> items) {
        exigirActivo(ambito, id);
        int total = 0, peliculas = 0, series = 0;
        for (ItemConsumption item : items) {
            if (AmbitoGenero.deTipo(item.getTipo()) != ambito || item.getGeneros().stream().noneMatch(r -> r.id().equals(id))) continue;
            total++;
            if (item instanceof Pelicula) peliculas++;
            else if (item instanceof Serie) series++;
        }
        return new Uso(total, peliculas, series);
    }
}