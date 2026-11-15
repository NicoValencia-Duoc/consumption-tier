package consumption.ui;

public enum Tema {
    NOCTURNO("nocturno"),
    SOLAR("solar"),
    NEON("neon"),
    TERMINAL("terminal"),
    OXIDO("oxido"),
    ABISMO("abismo");

    private final String archivo;

    Tema(String archivo) {
        this.archivo = archivo;
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