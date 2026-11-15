package consumption.ui;

public enum Tema {
    NOCTURNO("nocturno", "#08080a"),
    SOLAR("solar", "#f1ecd2"),
    NEON("neon", "#0b0614"),
    TERMINAL("terminal", "#020a04"),
    OXIDO("oxido", "#120a06"),
    ABISMO("abismo", "#040b14");

    private final String archivo;
    private final String fondo;

    Tema(String archivo, String fondo) {
        this.archivo = archivo;
        this.fondo = fondo;
    }

    public String getFondo() {
        return fondo;
    }

    public String getRecurso() {
        return "/consumption/temas/" + archivo + ".css";
    }

    public Tema siguiente() {
        Tema[] todos = values();
        return todos[(ordinal() + 1) % todos.length];
    }

    public static Tema de(String nombre) {
        if (nombre != null) {
            for (Tema t : values()) {
                if (t.name().equalsIgnoreCase(nombre.trim())) return t;
            }
        }
        return NOCTURNO;
    }
}