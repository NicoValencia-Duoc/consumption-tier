package consumption.model;

import java.util.Collection;
import java.util.Objects;

public enum NivelMaw {
    SIN_NIVEL(0), HAMBRIENTA(1), DESPIERTA(25), VORAZ(50), INSACIABLE(75), DEIDAD(100);

    private final int minimo;

    NivelMaw(int minimo) { this.minimo = minimo; }

    public int minimo() { return minimo; }

    public String etiqueta() { return this == SIN_NIVEL ? "SIN NIVEL" : name(); }

    public int progreso(long terminados) {
        if (this == SIN_NIVEL) return 0;
        if (this == DEIDAD) return 100;
        long ancho = values()[ordinal() + 1].minimo - minimo;
        long avance = Math.max(0, Math.min(ancho, terminados - minimo + 1));
        return (int) (avance * 100 / ancho);
    }

    public static NivelMaw porCantidad(long terminados) {
        if (terminados < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa");
        NivelMaw nivel = SIN_NIVEL;
        for (NivelMaw candidato : values()) {
            if (terminados >= candidato.minimo) nivel = candidato;
        }
        return nivel;
    }

    public static long contarTerminados(Collection<? extends ItemConsumption> items) {
        Objects.requireNonNull(items, "items");
        return items.stream().filter(Objects::nonNull)
                .filter(item -> item.getVecesConsumido() >= 1).count();
    }

    public static NivelMaw de(Collection<? extends ItemConsumption> items) {
        return porCantidad(contarTerminados(items));
    }
}
