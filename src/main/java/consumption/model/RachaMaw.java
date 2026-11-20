package consumption.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class RachaMaw {
    private RachaMaw() {}

    public enum TierRacha {
        SIN_TIER(0, null), F(6, "#9B6BFF"), D(12, "#2FD8FF"),
        C(18, "#39FF88"), B(24, "#FCEE0A"), A(30, "#FF8A1F"), S(100, "#FF2FA0");
        private final int diasMinimos;
        private final String color;
        TierRacha(int diasMinimos, String color) { this.diasMinimos = diasMinimos; this.color = color; }
        public String color() { return color; }
        public int diasMinimos() { return diasMinimos; }
    }

    public static int dias(Collection<? extends ItemConsumption> items, LocalDate hoy) {
        if (items == null || hoy == null) throw new IllegalArgumentException("Items y fecha obligatorios");
        Set<LocalDate> fechas = new HashSet<>();
        for (ItemConsumption item : items) {
            if (item == null) continue;
            LocalDate primera = item.getFechaPrimeraFinalizacion();
            if (primera != null && !primera.isAfter(hoy)) fechas.add(primera);
        }
        LocalDate cursor = fechas.contains(hoy) ? hoy : hoy.minusDays(1);
        int consecutivos = 0;
        while (fechas.contains(cursor)) {
            consecutivos++;
            cursor = cursor.minusDays(1);
        }
        return consecutivos;
    }

    public static TierRacha tier(int dias) {
        if (dias < 0) throw new IllegalArgumentException("Dias negativos");
        TierRacha resultado = TierRacha.SIN_TIER;
        for (TierRacha tier : TierRacha.values()) {
            if (dias >= tier.diasMinimos()) resultado = tier;
        }
        return resultado;
    }
}
