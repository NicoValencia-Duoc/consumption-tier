package consumption.model;

import java.time.LocalDate;

public abstract class ItemConsumption {

    private String titulo;
    private double nota;
    private Estado estado;
    private double porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;
    private String comentario = "";
    public static final int MAX_COMENTARIO = 500;

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

    public void setPorcentaje(double p) {
        if (p < 0 || p > getPorcentajeMaximo()){
            throw new IllegalArgumentException("El porcentaje de avance debe estar entrew 0 y "+ getPorcentajeMaximo()+ ".");
        }
        this.porcentaje = p;
    }

    public void setFechaInicio(LocalDate f) {
        this.fechaInicio = f;
    }

    public void setFechaTermino(LocalDate f) {
        if (f != null && fechaInicio != null && f.isBefore(fechaInicio)){
            throw new IllegalArgumentException("La fecha de termino no puede ser anterior a la del inicio.");
        }
        this.fechaTermino = f;
    }

    public void setComentario(String c){
        if (c != null && c.length() > MAX_COMENTARIO){
            throw new IllegalArgumentException("El comentario tiene un maximo de " + MAX_COMENTARIO + "caracteres.");
        }
        this.comentario = (c == null) ? "" : c;
    }
}
