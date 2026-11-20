package consumption.ui;

public enum NivelMaw {
    HAMBRIENTA, DESPIERTA, VORAZ, INSACIABLE, DEIDAD;

    private static final int SEGMENTOS = 5;

    public static NivelMaw de(int terminados) {
        return valueOf(consumption.model.NivelMaw.porCantidad(terminados).name());
    }

    public int progreso(int terminados) {
        if (this == DEIDAD) return 100;
        int avance = Math.max(0, terminados - consumption.model.NivelMaw.valueOf(name()).minimo());
        return Math.min(100, avance * 100 / 20);
    }

    public static String barra(int porcentaje) {
        int llenos = Math.round(Math.max(0, Math.min(100, porcentaje)) * SEGMENTOS / 100f);
        return "[" + "#".repeat(llenos) + "-".repeat(SEGMENTOS - llenos) + "]";
    }

    public static String pie(int terminados, int sinDevorar) {
        NivelMaw nivel = de(terminados);
        int porcentaje = nivel.progreso(terminados);
        return "THE MAW TE OBSERVA · NIVEL " + nivel.name()
                + " · HAMBRE " + barra(porcentaje) + " " + porcentaje + "%"
                + " · " + terminados + (terminados == 1 ? " DEVORADO" : " DEVORADOS")
                + " · " + sinDevorar + " SIN DEVORAR";
    }
}
