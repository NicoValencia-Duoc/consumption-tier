package consumption.model;

public class Pelicula extends ItemConsumption{

    public Pelicula(String titulo, double nota, Estado estado){
        super(titulo, nota, estado);
    }
    @Override public String getTipo(){
        return "PELICULAS";
    }
    @Override public String getLetra(){
        return "P";
    }

}