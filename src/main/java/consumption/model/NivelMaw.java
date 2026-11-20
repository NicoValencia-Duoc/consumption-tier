package consumption.model;

import java.util.Collection;
import java.util.Objects;

public enum NivelMaw {
    HAMBRIENTA(0), DESPIERTA(20), VORAZ(40), INSACIABLE(60), DEIDAD(80);

    private final int minimo;

    NivelMaw(int minimo) { this.minimo = minimo; }

    public int minimo() { return minimo; }

    public static NivelMaw porCantidad(long terminados) {
        if (terminados < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa");
        if (terminados >= 80) return DEIDAD;
        if (terminados >= 60) return INSACIABLE;
        if (terminados >= 40) return VORAZ;
        if (terminados >= 20) return DESPIERTA;
        return HAMBRIENTA;
    }

    public static long contarTerminados(Collection<? extends ItemConsumption> items) {
        Objects.requireNonNull(items, "items");
        return items.stream().filter(Objects::nonNull)
                .filter(item -> item.getEstado() == Estado.TERMINADO).count();
    }

    public static NivelMaw de(Collection<? extends ItemConsumption> items) {
        return porCantidad(contarTerminados(items));
    }
}
