package consumption.ui;

public enum NivelMaw {
    HAMBRIENTA(0, 10),
    DESPIERTA(10, 50),
    VORAZ(50, 100),
    INSACIABLE(100, Integer.MAX_VALUE);

    private static final int SEGMENTOS = 5;

    private final int desde;
    private final int hasta;

    NivelMaw(int desde, int hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public static NivelMaw de(int devorados) {
        NivelMaw nivel = HAMBRIENTA;
        for (NivelMaw n : values()) {
            if (devorados >= n.desde) nivel = n;
        }
        return nivel;
    }

    public int progreso(int devorados) {
        if (hasta == Integer.MAX_VALUE) return 100;
        int avance = Math.max(0, devorados - desde);
        return Math.min(100, avance * 100 / (hasta - desde));
    }

    public static String barra(int porcentaje) {
        int llenos = Math.round(Math.max(0, Math.min(100, porcentaje)) * SEGMENTOS / 100f);
        return "[" + "#".repeat(llenos) + "-".repeat(SEGMENTOS - llenos) + "]";
    }

    public static String pie(int devorados, int sinDevorar) {
        NivelMaw nivel = de(devorados);
        int porcentaje = nivel.progreso(devorados);
        return "THE MAW TE OBSERVA · NIVEL " + nivel.name()
                + " · HAMBRE " + barra(porcentaje) + " " + porcentaje + "%"
                + " · " + devorados + (devorados == 1 ? " DEVORADO" : " DEVORADOS")
                + " · " + sinDevorar + " SIN DEVORAR";
    }
}
