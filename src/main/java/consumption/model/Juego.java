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

    public Juego(String titulo, double nota, Estado estado, String plataforma, String genero, String desarrollador, int anio, double horasJugadas) {
        this(titulo, nota, estado, plataforma, genero, desarrollador, anio, horasJugadas, null, false);
    }

    public static Juego conGeneros(String titulo, double nota, Estado estado, String plataforma, List<ReferenciaGenero> referencias, String desarrollador, int anio, double horasJugadas) {
        return new Juego(titulo, nota, estado, plataforma, null, desarrollador, anio, horasJugadas, referencias, true);
    }

    private Juego(String titulo, double nota, Estado estado, String plataforma, String genero, String desarrollador, int anio, double horasJugadas, List<ReferenciaGenero> referencias, boolean migrado) {
        super(titulo, nota, estado);
        setPlataforma(plataforma);
        if (migrado) setGeneros(referencias);
        else setGenero(genero);
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
    @Override public double getPorcentajeMaximo(){return 200;}

    public void setPlataforma(String v) { this.plataforma = texto(v, "La plataforma", 40); }
    public void setGenero(String v) { this.genero = texto(v, "El genero", 40); }
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
    @Override public String textoBusqueda() {
        return unir(super.textoBusqueda(), getDesarrollador(), getPlataforma());
    }
    public int getAnio() {
        return anio;
    }
    public double getHorasJugadas() {
        return horasJugadas;
    }
}
