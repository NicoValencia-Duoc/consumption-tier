package consumption.ui;

public record Pista(String archivo, String titulo, String artista, String fuente) {

    static Pista desdeLinea(String linea) {
        String[] partes = linea.split("\\|", 4);
        String archivo = partes[0].trim();
        String titulo = partes.length > 1 ? partes[1].trim() : "";
        String artista = partes.length > 2 ? partes[2].trim() : "";
        String fuente = partes.length > 3 ? partes[3].trim() : "";
        if (titulo.isEmpty()) titulo = nombreLegible(archivo);
        return new Pista(archivo, titulo, artista, fuente);
    }

    public String textoCompleto() {
        return artista.isBlank() ? titulo : titulo + " — " + artista;
    }

    private static String nombreLegible(String archivo) {
        int punto = archivo.lastIndexOf('.');
        String sinExtension = (punto > 0) ? archivo.substring(0, punto) : archivo;
        return sinExtension.replace('_', ' ').toUpperCase();
    }
}
