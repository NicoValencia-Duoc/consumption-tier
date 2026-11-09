package consumption.model;

public class Pelicula extends ItemConsumption{

    private String director;
    private String genero;
    private int duracionMin;
    private int anio;

    public Pelicula(String titulo, double nota, Estado estado, String director, String genero, int duracionMin, int anio){
        super(titulo, nota, estado);
        setDirector(director);
        setGenero(genero);
        setDuracionMin(duracionMin);
        setAnio(anio);
    }

    @Override public String getTipo(){
        return "PELICULAS";
    }
    @Override public String getLetra(){
        return "P";
    }

    public void setDirector(String v) { this.director = texto(v, "El director", 60); }
    public void setGenero(String v) { this.genero = texto(v, "El género", 40); }
    public void setDuracionMin(int v) {
        if (v <= 0) throw new IllegalArgumentException("La duración debe ser más de 0 minutos");
        this.duracionMin = v;
    }
    public void setAnio(int v) { this.anio = anio(v); }

    public String getDirector() {
        return director;
    }
    public String getGenero() {
        return genero;
    }
    public int getDuracionMin() {
        return duracionMin;
    }
    public int getAnio() {
        return anio;
    }
}