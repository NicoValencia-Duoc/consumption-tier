package consumption.model;

public class Libro extends ItemConsumption{

    private String autor;
    private String genero;
    private int paginas;
    private int anio;

    public Libro(String titulo, double nota, Estado estado, String autor, String genero, int paginas, int anio){
        super(titulo, nota, estado);
        this.autor = autor;
        this.genero = genero;
        this.paginas = paginas;
        this.anio = anio;
    }

    @Override public String getTipo(){
        return "LIBROS";
    }
    @Override public String getLetra(){
        return "L";
    }

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