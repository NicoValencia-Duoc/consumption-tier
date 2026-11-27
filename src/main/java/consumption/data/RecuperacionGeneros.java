package consumption.data;

import consumption.model.*;
import java.util.*;

public final class RecuperacionGeneros {
    private RecuperacionGeneros() {}
    public record Candidato(int indice, String nombre, UUID idHistorico, boolean disponible) {}
    public record Resultado(BibliotecaGeneros biblioteca, List<ReferenciaGenero> referencias) {}

    public static final class Plan {
        private final BibliotecaGeneros biblioteca;
        private final AmbitoGenero ambito;
        private final List<Candidato> candidatos;
        private final List<ReferenciaGenero> originales;
        private final String legado;
        private final boolean migrado;
        private Plan(BibliotecaGeneros biblioteca, AmbitoGenero ambito, List<Candidato> candidatos,
                     List<ReferenciaGenero> originales, String legado, boolean migrado) {
            this.biblioteca = biblioteca; this.ambito = ambito; this.candidatos = List.copyOf(candidatos);
            this.originales = List.copyOf(originales); this.legado = legado; this.migrado = migrado;
        }
        public BibliotecaGeneros biblioteca() { return biblioteca; }
        public List<Candidato> candidatos() { return candidatos; }
        public boolean corresponde(ItemConsumption item) {
            return AmbitoGenero.deTipo(item.getTipo()) == ambito && item.tieneGenerosMigrados() == migrado
                    && (migrado ? originales.equals(item.getGeneros()) : Objects.equals(legado, item.getGenero()));
        }
        public Resultado resolver(Set<Integer> autorizados) {
            return resolverSobre(biblioteca, autorizados);
        }
        Resultado resolverSobre(BibliotecaGeneros acumulada, Set<Integer> autorizados) {
            Objects.requireNonNull(autorizados);
            Set<Integer> opcionales = new HashSet<>();
            for (Candidato c : candidatos) if (!c.disponible()) opcionales.add(c.indice());
            if (!opcionales.containsAll(autorizados)) throw new IllegalArgumentException("Seleccion de recuperacion invalida.");
            BibliotecaGeneros nueva = acumulada;
            Map<UUID, ReferenciaGenero> refs = new LinkedHashMap<>();
            for (Candidato c : candidatos) {
                if (!c.disponible() && !autorizados.contains(c.indice())) continue;
                TagGenero tag;
                if (c.disponible()) {
                    tag = nueva.exigirActivo(ambito, c.idHistorico());
                } else {
                    var alta = nueva.incorporarHistorico(ambito, c.nombre());
                    nueva = alta.biblioteca(); tag = alta.tag();
                }
                refs.putIfAbsent(tag.id(), ReferenciaGenero.de(tag));
            }
            return new Resultado(nueva, List.copyOf(refs.values()));
        }
    }
    public static Plan preparar(BibliotecaGeneros biblioteca, ItemConsumption item) {
        return preparar(biblioteca, AmbitoGenero.deTipo(item.getTipo()), item.getGeneros(), item.getGenero(), item.tieneGenerosMigrados());
    }
    public static Plan preparar(BibliotecaGeneros biblioteca, AmbitoGenero ambito,
                                List<ReferenciaGenero> referencias, String legado, boolean migrado) {
        List<Candidato> candidatos = new ArrayList<>();
        if (migrado) {
            for (ReferenciaGenero ref : referencias) {
                Optional<TagGenero> conocido = biblioteca.buscar(ref.id());
                if (conocido.isPresent() && (conocido.get().ambito() != ambito || !conocido.get().nombre().equals(ref.nombre())))
                    throw new IllegalArgumentException("Referencia historica contradictoria.");
                candidatos.add(new Candidato(candidatos.size(), ref.nombre(), ref.id(), conocido.filter(TagGenero::activo).isPresent()));
            }
        } else if (!NombreGenero.clave(legado).isEmpty()) {
            candidatos.add(new Candidato(0, NombreGenero.historico(legado), null, false));
        }
        return new Plan(biblioteca, ambito, candidatos, referencias, legado, migrado);
    }
}