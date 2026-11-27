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

    public Libro(String titulo, double nota, Estado estado, String autor, String genero, int paginas, int anio) {
        this(titulo, nota, estado, autor, genero, paginas, anio, null, false);
    }

    public static Libro conGeneros(String titulo, double nota, Estado estado, String autor, List<ReferenciaGenero> referencias, int paginas, int anio) {
        return new Libro(titulo, nota, estado, autor, null, paginas, anio, referencias, true);
    }

    private Libro(String titulo, double nota, Estado estado, String autor, String genero, int paginas, int anio, List<ReferenciaGenero> referencias, boolean migrado) {
        super(titulo, nota, estado);
        setAutor(autor);
        if (migrado) setGeneros(referencias);
        else setGenero(genero);
        setPaginas(paginas);
        setAnio(anio);
    }

    @Override public String getTipo(){
        return "LIBROS";
    }
    @Override public String getLetra(){
        return "L";
    }

    public void setAutor(String v) { this.autor = texto(v, "La autoria", 60); }
    public void setGenero(String v) { this.genero = texto(v, "El genero", 40); }
    public void setPaginas(int v) {
        if (v <= 0) throw new IllegalArgumentException("Las paginas deben ser mas de 0");
        this.paginas = v;
    }
    public void setAnio(int v) { this.anio = anio(v); }

    public String getAutor() {
        return autor;
    }
    @Override public String textoBusqueda() {
        return unir(super.textoBusqueda(), getAutor());
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