package consumption.model;

import java.util.List;

public class Libro extends ItemConsumption{

    public static final List<String> CRITERIOS =
            List.of("TRAMA", "PERSONAJES", "ESCRITURA", "FLUIDEZ", "IMPACTO PERSONAL");

    @Override public List<String> getNombresCriterios() { return CRITERIOS; }

    private String autor;
    private String genero;
    private int paginas;
    private int anio;

    public Libro(String titulo, double nota, Estado estado, String autor, String genero, int paginas, int anio){
        super(titulo, nota, estado);
        setAutor(autor);
        setGenero(genero);
        setPaginas(paginas);
        setAnio(anio);
    }

    @Override public String getTipo(){
        return "LIBROS";
    }
    @Override public String getLetra(){
        return "L";
    }

    public void setAutor(String v) { this.autor = texto(v, "El autor", 60); }
    public void setGenero(String v) { this.genero = texto(v, "El género", 40); }
    public void setPaginas(int v) {
        if (v <= 0) throw new IllegalArgumentException("Las páginas deben ser más de 0");
        this.paginas = v;
    }
    public void setAnio(int v) { this.anio = anio(v); }

    public String getAutor() {
        return autor;
    }
    public String getGenero() {
        return genero;
    }
    public int getPaginas() {
        return paginas;
    }
    public int getAnio() {
        return anio;
    }
}