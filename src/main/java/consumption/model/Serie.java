package consumption.model;

import java.util.List;

public class Serie extends ItemConsumption {

    public static final List<String> CRITERIOS =
            List.of("GUION", "PERSONAJES", "RITMO", "PRODUCCION", "FINAL");

    @Override public List<String> getNombresCriterios() { return CRITERIOS; }

    private String creador;
    private String genero;
    private int anio;
    private int temporadas;
    private int episodios;
    private String origen = "";

    public Serie(String titulo, double nota, Estado estado, String creador, String genero, int anio, int temporadas, int episodios, String origen) {

        super(titulo, nota, estado);
        setCreador(creador);
        setGenero(genero);
        setAnio(anio);
        setTemporadas(temporadas);
        setEpisodios(episodios);
        setOrigen(origen);
    }

    @Override public String getTipo() {
        return "SERIES";
    }
    @Override public String getLetra() {
        return "T";
    }

    public void setCreador(String v) { this.creador = texto(v, "El creador", 60); }
    public void setGenero(String v) { this.genero = texto(v, "El genero", 40); }
    public void setAnio(int v) { this.anio = anio(v); }
    public void setTemporadas(int v) {
        if (v <= 0) throw new IllegalArgumentException("Las temporadas deben ser mas de 0.");
        this.temporadas = v;
    }
    public void setEpisodios(int v) {
        if (v <= 0) throw new IllegalArgumentException("Los episodios deben ser mas de 0.");
        this.episodios = v;
    }
    public void setOrigen(String v) {
        String t = (v == null) ? "" : v.trim();
        if (t.length() > 40) throw new IllegalArgumentException("El origen admite maximo 40 caracteres.");
        this.origen = t;
    }

    @Override public void validarCoherencia() {
        super.validarCoherencia();
        if (episodios < temporadas) {
            throw new IllegalArgumentException("Una serie tiene al menos un episodio por temporada.");
        }
    }

    public String getCreador() { return creador; }
    @Override public String getGenero() { return genero; }
    public int getAnio() { return anio; }
    public int getTemporadas() { return temporadas; }
    public int getEpisodios() { return episodios; }
    public String getOrigen() { return origen == null ? "" : origen; }
}