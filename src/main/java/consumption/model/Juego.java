package consumption.model;

public class Juego extends ItemConsumption{

    private String plataforma;
    private String genero;
    private String desarrollador;
    private int anio;
    private double horasJugadas;

    public Juego(String titulo, double nota, Estado estado, String plataforma, String genero, String desarrollador, int anio, double horasJugadas){
        super(titulo, nota, estado);
        this.plataforma = plataforma;
        this.genero = genero;
        this.desarrollador = desarrollador;
        this.anio = anio;
        this.horasJugadas = horasJugadas;
    }

    @Override public String getTipo(){
        return "JUEGOS";
    }
    @Override public String getLetra(){
        return "J";
    }
    @Override public double getPorcentajeMaximo(){return 999;}

    public String getPlataforma(){
        return plataforma;
    }
    public String getGenero(){
        return genero;
    }
    public String getDesarrollador(){
        return desarrollador;
    }
    public int getAnio() {
        return anio;
    }
    public double getHorasJugadas() {
        return horasJugadas;
    }
}
