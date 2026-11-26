package consumption.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class GraficosEstadisticas {
    private static final String[] MESES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};

    public record BarraTier(Tier tier, int cantidad, double porcentaje) { }

    public record Distribucion(List<BarraTier> barras, int total) { }

    public record Barra(String etiqueta, int valor) { }

    private GraficosEstadisticas() { }

    public static Distribucion distribucionTiers(List<? extends ItemConsumption> items) {
        int[] cantidades = new int[Tier.values().length];
        int total = 0;
        for (ItemConsumption item : items) {
            if (item.getEstado() == Estado.PENDIENTE) continue;
            Tier tier = item.getTier();
            if (tier == null) continue;
            cantidades[tier.ordinal()]++;
            total++;
        }
        List<BarraTier> barras = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            int cantidad = cantidades[tier.ordinal()];
            barras.add(new BarraTier(tier, cantidad, total == 0 ? 0 : cantidad * 100.0 / total));
        }
        return new Distribucion(barras, total);
    }

    public static List<Barra> rankingGeneros(List<? extends ItemConsumption> items, int tope) {
        Map<String, Integer> cuentas = new LinkedHashMap<>();
        Map<String, String> nombres = new LinkedHashMap<>();
        for (ItemConsumption item : items) {
            if (item.getEstado() == Estado.PENDIENTE) continue;
            String genero = item.getGenero();
            if (genero == null || genero.isBlank()) continue;
            String clave = ReglasInicio.normalizar(genero);
            cuentas.merge(clave, 1, Integer::sum);
            nombres.putIfAbsent(clave, genero.trim().toUpperCase(Locale.ROOT));
        }
        List<Barra> ranking = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cuentas.entrySet()) {
            ranking.add(new Barra(nombres.get(e.getKey()), e.getValue()));
        }
        ranking.sort(Comparator.comparingInt(Barra::valor).reversed().thenComparing(Barra::etiqueta));
        return ranking.size() <= tope ? ranking : new ArrayList<>(ranking.subList(0, tope));
    }

    public static List<Barra> porAnio(List<? extends ItemConsumption> items, int maximo) {
        TreeMap<Integer, Integer> cuentas = new TreeMap<>();
        for (ItemConsumption item : items) {
            LocalDate fecha = fechaDeCierre(item);
            if (fecha != null) cuentas.merge(fecha.getYear(), 1, Integer::sum);
        }
        List<Barra> anios = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cuentas.entrySet()) {
            anios.add(new Barra(String.valueOf(e.getKey()), e.getValue()));
        }
        int desde = Math.max(0, anios.size() - maximo);
        return new ArrayList<>(anios.subList(desde, anios.size()));
    }

    public static List<Barra> porMes(List<? extends ItemConsumption> items, LocalDate hoy) {
        YearMonth actual = YearMonth.from(hoy);
        int[] cuentas = new int[12];
        for (ItemConsumption item : items) {
            LocalDate fecha = fechaDeCierre(item);
            if (fecha == null) continue;
            long atras = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(fecha), actual);
            if (atras >= 0 && atras < 12) cuentas[11 - (int) atras]++;
        }
        List<Barra> meses = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth mes = actual.minusMonths(11 - i);
            meses.add(new Barra(MESES[mes.getMonthValue() - 1], cuentas[i]));
        }
        return meses;
    }

    private static LocalDate fechaDeCierre(ItemConsumption item) {
        if (item.getEstado() != Estado.TERMINADO && item.getEstado() != Estado.ABANDONADO) return null;
        return item.getFechaTermino();
    }
}
