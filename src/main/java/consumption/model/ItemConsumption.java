package consumption.model;

import java.time.LocalDate;

public abstract class ItemConsumption {

    public static final int MAX_COMENTARIO = 500;
    private String titulo;
    private double nota;
    private Estado estado;
    private double porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;
    private String comentario = "";

    public ItemConsumption(String titulo, double nota, Estado estado){

        setTitulo(titulo);
        setNota(nota);
        setEstado(estado);
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
    public double getPorcentajeMaximo() {
        return 100;
    }
    public double getPorcentaje() {
        return porcentaje;
    }
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    public LocalDate getFechaTermino() {
        return fechaTermino;
    }
    public String getComentario() {
        return comentario;
    }

    public void setTitulo(String t){
        this.titulo = texto(t, "El titulo", 80);
    }

    public void setNota(double n) {
        if (n < 0 || n > 10) throw new IllegalArgumentException("La nota debe estar entre 0 y 10");
        this.nota = n;
    }

    public void setEstado(Estado e){
        if (e == null) throw new IllegalArgumentException("Elige un estado");
        this.estado = e;
    }

    public void setPorcentaje(double p) {
        if (p < 0 || p > getPorcentajeMaximo()){
            throw new IllegalArgumentException("El porcentaje de avance debe estar entre 0 y "+ getPorcentajeMaximo()+ ".");
        }
        this.porcentaje = p;
    }

    public void setFechaInicio(LocalDate f) {
        if (f != null && fechaTermino != null && fechaTermino.isBefore(f)){
            throw new IllegalArgumentException("La fecha de inicio no puede ser despues de la de termino.");
        }
        this.fechaInicio = f;
    }

    public void setFechaTermino(LocalDate f) {
        if (f != null && fechaInicio != null && f.isBefore(fechaInicio)){
            throw new IllegalArgumentException("La fecha de termino no puede ser anterior a la de inicio.");
        }
        this.fechaTermino = f;
    }

    public void setComentario(String c){
        if (c != null && c.length() > MAX_COMENTARIO){
            throw new IllegalArgumentException("El comentario tiene un maximo de " + MAX_COMENTARIO + "caracteres.");
        }
        this.comentario = (c == null) ? "" : c;
    }

    public void validarCoherencia(){
        if(estado == Estado.TERMINADO && fechaTermino == null){
            throw new IllegalArgumentException("Un item terminado necesita fecha de termino.");
        }if (estado == Estado.PENDIENTE && (porcentaje > 0 || fechaInicio != null || fechaTermino != null)){
            throw new IllegalArgumentException("Un item pendiente no puede tener avance ni fechas.");
        }
    }

    protected static String texto(String v, String campo, int max){
        String t =(v == null) ? "" : v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(campo + " es obligatorio.");
        if (t.length() > max) throw new IllegalArgumentException(campo + " admite maximo " + max + " caracteres.");
        return t;
    }

    protected static int anio(int v){
        int tope = LocalDate.now().getYear() + 1;
        if (v < 1800 || v > tope) throw new IllegalArgumentException("El año debe estar entre 1800 y \" + tope");
        return v;
    }
}
