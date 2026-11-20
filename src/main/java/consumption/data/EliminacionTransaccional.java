package consumption.data;

import consumption.model.ItemConsumption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EliminacionTransaccional {
    private EliminacionTransaccional() {}

    public static Set<String> ejecutar(List<ItemConsumption> biblioteca,
                                       List<? extends ItemConsumption> seleccion,
                                       RepositorioJson repositorio,
                                       Papelera papelera) throws Exception {
        List<ItemConsumption> lote = new ArrayList<>(seleccion);
        if (lote.isEmpty()) return Set.of();

        Set<ItemConsumption> unicos = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemConsumption item : lote) {
            if (item == null || !unicos.add(item))
                throw new IllegalArgumentException("Seleccion duplicada o invalida");
            long coincidencias = biblioteca.stream().filter(existente -> existente == item).count();
            if (coincidencias != 1)
                throw new IllegalArgumentException("La seleccion no pertenece de forma unica a la biblioteca");
        }
        List<ItemConsumption> restantes = new ArrayList<>(biblioteca);
        if (!restantes.removeAll(lote) || biblioteca.size() - restantes.size() != lote.size())
            throw new IllegalArgumentException("La seleccion no coincide con la biblioteca");
        Set<String> anteriores = new HashSet<>();
        for (Papelera.Entrada entrada : papelera.listar()) anteriores.add(entrada.id());
        try {
            DiarioRestauracion.ejecutar(() -> {
                papelera.enviar(lote);
                repositorio.guardar(restantes);
            });
        } catch (Exception fallo) {
            try {
                papelera.recargar();
            } catch (RuntimeException recarga) {
                fallo.addSuppressed(recarga);
            }
            throw fallo;
        }
        Set<String> nuevos = new HashSet<>();
        for (Papelera.Entrada entrada : papelera.listar())
            if (!anteriores.contains(entrada.id())) nuevos.add(entrada.id());
        biblioteca.clear();
        biblioteca.addAll(restantes);
        return Set.copyOf(nuevos);
    }
}
