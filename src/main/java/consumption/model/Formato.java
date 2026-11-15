package consumption.model;

import java.util.Locale;

public final class Formato {

    private static final Locale ES = Locale.forLanguageTag("es-CL");

    private Formato() {
    }

    public static String decimal(double valor) {
        return String.format(ES, "%.1f", valor);
    }
}