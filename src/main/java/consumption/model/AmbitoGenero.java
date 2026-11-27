package consumption.model;

public enum AmbitoGenero {
    JUEGOS("JUEGOS"), LIBROS("LIBROS"), AUDIOVISUAL("PELICULAS + SERIES");

    private final String etiqueta;
    AmbitoGenero(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }

    public static AmbitoGenero deTipo(String tipo) {
        return switch (tipo) {
            case "JUEGOS" -> JUEGOS;
            case "LIBROS" -> LIBROS;
            case "PELICULAS", "SERIES" -> AUDIOVISUAL;
            default -> throw new IllegalArgumentException("Tipo sin biblioteca de generos: " + tipo);
        };
    }
}
