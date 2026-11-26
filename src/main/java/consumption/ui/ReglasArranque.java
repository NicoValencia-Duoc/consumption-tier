package consumption.ui;

final class ReglasArranque {
    static final double OMITIBLE_DESDE_MS = 1000;

    private ReglasArranque() { }

    static boolean omitible(double milisegundosTranscurridos) {
        return milisegundosTranscurridos >= OMITIBLE_DESDE_MS;
    }
}
