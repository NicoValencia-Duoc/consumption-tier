package consumption.model;

public class Libro extends ItemConsumption{

    public Libro(String titulo, double nota, Estado estado){
        super(titulo, nota, estado);
    }
    @Override public String getTipo(){
        return "LIBROS";
    }
    @Override public String getLetra(){
        return "L";
    }

}