package consumption.model;

public class Pelicula extends ItemConsumption{

    private String director;
    private String genero;
    private int duracionMin;
    private int anio;

    public Pelicula(String titulo, double nota, Estado estado, String director, String genero, int duracionMin, int anio){
        super(titulo, nota, estado);
        this.director = director;
        this.genero = genero;
        this.duracionMin = duracionMin;
        this.anio = anio;
    }

    @Override public String getTipo(){
        return "PELICULAS";
    }
    @Override public String getLetra(){
        return "P";
    }

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