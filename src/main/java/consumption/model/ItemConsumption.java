package consumption.model;

public abstract class ItemConsumption {

    private String titulo;
    private double nota;
    private Estado estado;

    public ItemConsumption(String titulo, double nota, Estado estado){

        this.titulo = titulo;
        this.nota = nota;
        this.estado = estado;
    }

    public abstract String getTipo();
    public abstract String getLetra();

    public String getTitulo(){
        return titulo;
    }
    public double getNota(){
        return nota;
    }
    public Estado getEstado(){
        return estado;
    }
}
