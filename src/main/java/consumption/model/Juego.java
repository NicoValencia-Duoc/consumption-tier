package consumption.model;

public class Juego extends ItemConsumption{

    public Juego(String titulo, double nota, Estado estado){
        super(titulo, nota, estado);
    }
    @Override public String getTipo(){
        return "JUEGOS";
    }
    @Override public String getLetra(){
        return "J";
    }

}
