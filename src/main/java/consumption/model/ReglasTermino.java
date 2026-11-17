package consumption.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ReglasTermino {
    private ReglasTermino() {}

    public static String textoDias(ItemConsumption item, LocalDate hoy) {
        if (item == null || hoy == null || item.getEstado() != Estado.TERMINADO
                || item.getFechaTermino() == null) {
            return null;
        }
        long dias = ChronoUnit.DAYS.between(item.getFechaTermino(), hoy);
        if (dias < 0) return null;
        if (dias == 0) return "TERMINADO HOY";
        if (dias == 1) return "TERMINADO AYER";
        return "TERMINADO HACE " + dias + " DIAS";
    }
}