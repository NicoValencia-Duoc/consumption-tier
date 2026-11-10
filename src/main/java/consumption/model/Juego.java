package consumption.model;

import java.util.List;

public class Juego extends ItemConsumption{

    public static final List<String> CRITERIOS =
            List.of("JUGABILIDAD", "HISTORIA", "GRAFICOS", "MUSICA Y SONIDO", "REJUGABILIDAD");

    @Override public List<String> getNombresCriterios() { return CRITERIOS; }

    private String plataforma;
    private String genero;
    private String desarrollador;
    private int anio;
    private double horasJugadas;

    public Juego(String titulo, double nota, Estado estado, String plataforma, String genero, String desarrollador, int anio, double horasJugadas){
        super(titulo, nota, estado);
        setPlataforma(plataforma);
        setGenero(genero);
        setDesarrollador(desarrollador);
        setAnio(anio);
        setHorasJugadas(horasJugadas);
    }

    @Override public String getTipo(){
        return "JUEGOS";
    }
    @Override public String getLetra(){
        return "J";
    }
    @Override public double getPorcentajeMaximo(){return 999;}

    public void setPlataforma(String v) { this.plataforma = texto(v, "La plataforma", 40); }
    public void setGenero(String v) { this.genero = texto(v, "El género", 40); }
    public void setDesarrollador(String v) { this.desarrollador = texto(v, "El desarrollador", 60); }
    public void setAnio(int v) { this.anio = anio(v); }
    public void setHorasJugadas(double v) {
        if (v < 0) throw new IllegalArgumentException("Las horas no pueden ser negativas");
        this.horasJugadas = v;
    }

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
