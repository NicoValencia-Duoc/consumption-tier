package consumption.ui;

public final class Nombres {

    private Nombres() {
    }
    public static String singular(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case "JUEGOS" -> "JUEGO";
            case "LIBROS" -> "LIBRO";
            case "PELICULAS" -> "PELICULA";
            case "SERIES" -> "SERIE";
            default -> tipo;
        };
    }
}