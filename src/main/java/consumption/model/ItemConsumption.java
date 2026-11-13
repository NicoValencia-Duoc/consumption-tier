package consumption.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemConsumption {

    public static final int MAX_COMENTARIO = 500;
    private String titulo;
    private double nota;
    private Estado estado;
    private double porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;
    private String comentario = "";
    private Map<String, Double> criterios = new LinkedHashMap<>();
    private boolean favorito;
    private String rutaImagen;

    public ItemConsumption(String titulo, double nota, Estado estado){

        setTitulo(titulo);
        setNota(nota);
        setEstado(estado);
    }

    public abstract String getTipo();
    public abstract String getLetra();
    public abstract String getGenero();

    public String getTitulo(){
        return titulo;
    }
    public double getNota(){
        return nota;
    }
    public Tier getTier() {
        boolean sinNota = nota <= 0 && getCriterios().isEmpty();
        return sinNota ? null : Tier.de(nota);
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
    public String getRutaImagen() { return rutaImagen; }

    public boolean isFavorito() { return favorito; }

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

    public void setFavorito(boolean f) {
        this.favorito = f;
    }

    public void setComentario(String c){
        if (c != null && c.length() > MAX_COMENTARIO){
            throw new IllegalArgumentException("El comentario tiene un maximo de " + MAX_COMENTARIO + "caracteres.");
        }
        this.comentario = (c == null) ? "" : c;
    }

    public void setRutaImagen(String r) {
        this.rutaImagen = r;
    }

    public abstract List<String> getNombresCriterios();

    public Map<String, Double> getCriterios() {
        return Collections.unmodifiableMap(mapa());
    }

    public void setCriterio(String nombre, Double valor) {
        if (!getNombresCriterios().contains(nombre)) {
            throw new IllegalArgumentException("Criterio desconocido: " + nombre + ".");
        }
        if (valor == null) {
            mapa().remove(nombre);
        } else {
            if (valor < 0 || valor > 10) {
                throw new IllegalArgumentException(nombre + " debe estar entre 0 y 10.");
            }
            mapa().put(nombre, valor);
        }
        if (!mapa().isEmpty()) {
            double promedio = mapa().values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            this.nota = Math.round(promedio * 10) / 10.0;
        }
    }

    private Map<String, Double> mapa() {
        if (criterios == null) criterios = new LinkedHashMap<>();
        return criterios;
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
        if (v < 1800 || v > tope) throw new IllegalArgumentException("El año debe estar entre 1800 y " + tope + ".");
        return v;
    }
}
