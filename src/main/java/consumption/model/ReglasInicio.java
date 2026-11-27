package consumption.model;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

public final class ReglasInicio {

    private ReglasInicio() {
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinMarcas = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinMarcas.toLowerCase().trim();
    }

    public static boolean coincideBusqueda(ItemConsumption item, String consulta) {
        if (item.tieneGenerosMigrados()) return ConsultaGeneros.analizar(consulta).coincideInicio(item);
        String q = normalizar(consulta);
        if (q.isEmpty()) return true;
        String texto = normalizar(item.textoBusqueda());
        for (String palabra : q.split("\\s+")) {
            if (!texto.contains(palabra)) return false;
        }
        return true;
    }

    public static boolean pasaTier(ItemConsumption item, Tier tier) {
        if (tier == null) return true;
        if (item.getEstado() == Estado.PENDIENTE) return false;
        return item.getTier() == tier;
    }

    public static Integer diasEnCurso(ItemConsumption item, LocalDate hoy) {
        if (item.getEstado() != Estado.EN_PROGRESO || item.getFechaInicio() == null) return null;
        long dias = ChronoUnit.DAYS.between(item.getFechaInicio(), hoy);
        return dias < 0 ? null : (int) dias;
    }

    public static String textoDias(ItemConsumption item, LocalDate hoy) {
        Integer dias = diasEnCurso(item, hoy);
        if (dias == null) return null;
        if (dias == 0) return "EMPEZO HOY";
        return "LLEVA " + dias + (dias == 1 ? " DIA" : " DIAS");
    }

    public static Comparator<ItemConsumption> pendientesAlFinal(Comparator<ItemConsumption> elegido) {
        Comparator<ItemConsumption> alfabetico =
                Comparator.comparing(ItemConsumption::getTitulo, String.CASE_INSENSITIVE_ORDER);
        return (a, b) -> {
            boolean pa = a.getEstado() == Estado.PENDIENTE;
            boolean pb = b.getEstado() == Estado.PENDIENTE;
            if (pa != pb) return pa ? 1 : -1;
            return pa ? alfabetico.compare(a, b) : elegido.compare(a, b);
        };
    }
}