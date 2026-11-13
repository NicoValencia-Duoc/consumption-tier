package consumption.model;

public enum Tier {
    S("S", 9.5, "tier-s"),
    A("A", 8.5, "tier-a"),
    B("B", 7.5, "tier-b"),
    C("C", 6.0, "tier-c"),
    D("D", 4.0, "tier-d"),
    F("F", 0.0, "tier-f");

    private final String letra;
    private final double minimo;
    private final String claseCss;

    Tier(String letra, double minimo, String claseCss) {
        this.letra = letra;
        this.minimo = minimo;
        this.claseCss = claseCss;
    }

    public String getLetra() {
        return letra;
    }

    public double getMinimo() {
        return minimo;
    }

    public String getClaseCss() {
        return claseCss;
    }

    public static Tier de(double nota) {
        for (Tier t : values()) {
            if (nota >= t.minimo) return t;
        }
        return F;
    }
}
