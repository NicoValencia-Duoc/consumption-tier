package consumption.model;

import java.util.Comparator;
import java.util.List;

public class Estadisticas {

    public record Resumen(String nombre, int total, int terminados, int enProgreso,
                          int abandonados, int pendientes, double notaPromedio, String mejor) {}

    public static Resumen resumir(String nombre, List<? extends ItemConsumption> items) {

        List<? extends ItemConsumption> conNota = items.stream()
                .filter(i -> i.getEstado() != Estado.PENDIENTE && i.getNota() > 0)
                .toList();

        double promedio = conNota.stream().mapToDouble(ItemConsumption::getNota).average().orElse(0);
        String mejor = conNota.stream()
                .max(Comparator.comparingDouble(ItemConsumption::getNota))
                .map(i -> i.getTitulo() + " (" + String.format("%.1f", i.getNota()) + ")")
                .orElse("-");

        return new Resumen(nombre, items.size(),
                contar(items, Estado.TERMINADO), contar(items, Estado.EN_PROGRESO),
                contar(items, Estado.ABANDONADO), contar(items, Estado.PENDIENTE),
                promedio, mejor);
    }

    private static int contar(List<? extends ItemConsumption> items, Estado estado) {
        return (int) items.stream().filter(i -> i.getEstado() == estado).count();
    }
}