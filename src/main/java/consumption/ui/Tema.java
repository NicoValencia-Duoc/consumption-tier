package consumption.ui;

public enum Tema {
    INDUSTRIAL_DIURNO("solar", "#ede8d9", "SOLAR"),
    INDUSTRIAL_NOCTURNO("nocturno", "#0c0c0e", "ECLIPSE"),
    UMBRAL_NOCTURNO("neon", "#10111d", "ARCANO"),
    MAQUINA_ABISAL("abismo", "#071713", "ABISAL"),
    ACERO_FANTASMA("oxido", "#181c21", "ACERO"),
    TERMINAL_ESPECTRAL("terminal", "#080d16", "VECTOR");

    private final String archivo;
    private final String fondo;
    private final String nombre;

    Tema(String archivo, String fondo, String nombre) {
        this.archivo = archivo;
        this.fondo = fondo;
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
    public String getFondo() { return fondo; }
    public String getRecurso() { return "/consumption/temas/" + archivo + ".css"; }

    public Tema siguiente() {
        Tema[] todos = values();
        return todos[(ordinal() + 1) % todos.length];
    }

    public static Tema de(String nombre) {
        if (nombre != null) {
            String n = nombre.trim();
            for (Tema t : values()) {
                if (t.name().equalsIgnoreCase(n) || t.nombre.equalsIgnoreCase(n)) return t;
            }
            return switch (n.toUpperCase(java.util.Locale.ROOT)) {
                case "SOLAR", "INDUSTRIAL DIURNO" -> INDUSTRIAL_DIURNO;
                case "ECLIPSE", "NOCTURNO", "INDUSTRIAL NOCTURNO" -> INDUSTRIAL_NOCTURNO;
                case "ARCANO", "NEON", "UMBRAL NOCTURNO" -> UMBRAL_NOCTURNO;
                case "ABISAL", "ABISMO", "MÁQUINA ABISAL", "MAQUINA ABISAL" -> MAQUINA_ABISAL;
                case "ACERO", "OXIDO", "ACERO FANTASMA" -> ACERO_FANTASMA;
                case "VECTOR", "TERMINAL", "TERMINAL ESPECTRAL" -> TERMINAL_ESPECTRAL;
                default -> INDUSTRIAL_DIURNO;
            };
        }
        return INDUSTRIAL_DIURNO;
    }
}