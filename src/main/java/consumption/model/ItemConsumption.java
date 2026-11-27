package consumption.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Objects;

public abstract class ItemConsumption {

    public static final int MAX_COMENTARIO = 500;
    public static final int MAX_VECES = 999;
    public static final int MAX_CON_QUIEN = 40;
    private String titulo;
    private double nota;
    private Estado estado;
    private double porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;
    private LocalDate fechaPrimeraFinalizacion;
    private List<LocalDate> fechasTermino;
    private String comentario = "";
    private Map<String, Double> criterios = new LinkedHashMap<>();
    private boolean favorito;
    private String rutaImagen;
    private int vecesRepetido;
    private String conQuien = "";
    private Integer vecesConsumido;
    private boolean sinFechaInicio;
    private boolean sinFechaTermino;
    private int numero;
    private transient List<ReferenciaGenero> generos;

    public ItemConsumption(String titulo, double nota, Estado estado){

        setTitulo(titulo);
        setNota(nota);
        setEstado(estado);
    }

    public record EstadoAnterior(Estado estado, double porcentaje, LocalDate inicio,
                                 LocalDate termino, boolean sinInicio, boolean sinTermino,
                                 Integer consumos, int repeticiones, LocalDate primeraFinalizacion,
                                 List<LocalDate> fechasTermino) {}

    public EstadoAnterior capturarEstado() {
        return new EstadoAnterior(estado, porcentaje, fechaInicio, fechaTermino,
                sinFechaInicio, sinFechaTermino, vecesConsumido, vecesRepetido, fechaPrimeraFinalizacion,
                fechasTermino == null ? null : new ArrayList<>(fechasTermino));
    }

    public void restaurarEstado(EstadoAnterior anterior) {
        this.estado = anterior.estado();
        this.porcentaje = anterior.porcentaje();
        this.fechaInicio = anterior.inicio();
        this.fechaTermino = anterior.termino();
        this.sinFechaInicio = anterior.sinInicio();
        this.sinFechaTermino = anterior.sinTermino();
        this.vecesConsumido = anterior.consumos();
        this.vecesRepetido = anterior.repeticiones();
        this.fechaPrimeraFinalizacion = anterior.primeraFinalizacion();
        this.fechasTermino = anterior.fechasTermino() == null ? null : new ArrayList<>(anterior.fechasTermino());
    }
    public abstract String getTipo();
    public abstract String getLetra();
    public abstract String getGenero();

    public String generosTexto() {
        return tieneGenerosMigrados() ? getGeneros().stream().map(ReferenciaGenero::nombre)
                .collect(java.util.stream.Collectors.joining(", ")) : java.util.Objects.toString(getGenero(), "");
    }

    public boolean tieneGenerosMigrados() { return generos != null; }

    public List<ReferenciaGenero> getGeneros() {
        return generos == null ? List.of() : generos;
    }

    public void setGeneros(List<ReferenciaGenero> referencias) {
        Objects.requireNonNull(referencias, "referencias");
        var identidades = new HashSet<UUID>();
        for (ReferenciaGenero referencia : referencias) {
            Objects.requireNonNull(referencia, "Referencia de genero vacia.");
            if (!identidades.add(referencia.id()))
                throw new IllegalArgumentException("Referencia de genero duplicada.");
        }
        this.generos = List.copyOf(referencias);
    }


    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = Math.max(0, numero);
    }

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
        if (estado == Estado.TERMINADO && getPorcentajeMaximo() <= 100) return 100;
        return porcentaje;
    }
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    public LocalDate getFechaPrimeraFinalizacion() {
        if (fechaPrimeraFinalizacion != null) return fechaPrimeraFinalizacion;
        return estado == Estado.TERMINADO && getVecesConsumido() == 1
                ? fechaTermino : null;
    }
    public void setFechaPrimeraFinalizacion(LocalDate fecha) {
        this.fechaPrimeraFinalizacion = fecha;
    }
    public List<LocalDate> getFechasTermino() {
        TreeSet<LocalDate> fechas = new TreeSet<>();
        if (fechasTermino != null) {
            for (LocalDate fecha : fechasTermino) {
                if (fecha != null) fechas.add(fecha);
            }
        }
        LocalDate primera = getFechaPrimeraFinalizacion();
        if (primera != null) fechas.add(primera);
        return List.copyOf(fechas);
    }
    public void agregarFechaTermino(LocalDate fecha) {
        if (fecha == null) return;
        if (fechasTermino == null) fechasTermino = new ArrayList<>();
        fechasTermino.add(fecha);
    }
    public void setFechasTermino(List<LocalDate> fechas) {
        this.fechasTermino = fechas == null ? null : new ArrayList<>(fechas);
    }
    public LocalDate getFechaTermino() {
        return fechaTermino;
    }
    public String getComentario() {
        return comentario == null ? "" : comentario;
    }
    public String getConQuien() {
        return conQuien == null ? "" : conQuien;
    }

    public boolean isSinFechaInicio() { return sinFechaInicio; }
    public boolean isSinFechaTermino() { return sinFechaTermino; }

    public int getVecesConsumido() {
        int v = (vecesConsumido != null) ? vecesConsumido : vecesRepetido + (estado == Estado.TERMINADO ? 1 : 0);
        return Math.min(v, MAX_VECES);
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
            throw new IllegalArgumentException("El porcentaje de avance debe estar entre 0 y " + (int) getPorcentajeMaximo() + ".");
        }
        this.porcentaje = p;
    }

    public void setFechaInicio(LocalDate f) {
        if (f != null && fechaTermino != null && fechaTermino.isBefore(f)){
            throw new IllegalArgumentException("La fecha de inicio no puede ser despues de la de termino.");
        }
        this.fechaInicio = f;
        if (f != null) sinFechaInicio = false;
    }

    public void setFechaTermino(LocalDate f) {
        if (f != null && fechaInicio != null && f.isBefore(fechaInicio)){
            throw new IllegalArgumentException("La fecha de termino no puede ser anterior a la de inicio.");
        }
        this.fechaTermino = f;
        if (f != null) sinFechaTermino = false;
    }

    public void setSinFechaInicio(boolean v) {
        this.sinFechaInicio = v;
        if (v) fechaInicio = null;
    }

    public void setSinFechaTermino(boolean v) {
        this.sinFechaTermino = v;
        if (v) fechaTermino = null;
    }

    public void setFavorito(boolean f) {
        this.favorito = f;
    }

    public void setComentario(String c){
        if (c != null && c.length() > MAX_COMENTARIO){
            throw new IllegalArgumentException("El comentario tiene un maximo de " + MAX_COMENTARIO + " caracteres.");
        }
        this.comentario = (c == null) ? "" : c;
    }

    public void setRutaImagen(String r) {
        this.rutaImagen = r;
    }

    public void setConQuien(String c) {
        String t = (c == null) ? "" : c.trim();
        if (t.length() > MAX_CON_QUIEN) {
            throw new IllegalArgumentException("Con quien admite maximo " + MAX_CON_QUIEN + " caracteres.");
        }
        this.conQuien = t;
    }

    public void setVecesConsumido(int v) {
        if (v < 0 || v > MAX_VECES) {
            throw new IllegalArgumentException("Las veces consumidas deben estar entre 0 y " + MAX_VECES + ".");
        }
        this.vecesConsumido = v;
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
        if (estado == Estado.TERMINADO && fechaTermino == null && !sinFechaTermino) {
            throw new IllegalArgumentException("Un item terminado necesita fecha de termino o marcar SIN REGISTRO.");
        }
        if (estado == Estado.PENDIENTE && (porcentaje > 0 || fechaInicio != null || fechaTermino != null
                || sinFechaInicio || sinFechaTermino)) {
            throw new IllegalArgumentException("Un item pendiente no puede tener avance ni fechas.");
        }
    }

    public String textoBusqueda() {
        return unir(titulo, tieneGenerosMigrados() ? "" : getGenero(), comentario);
    }

    protected static String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.trim());
        }
        return sb.toString();
    }

    protected static String texto(String v, String campo, int max){
        String t = (v == null) ? "" : v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(campo + " es obligatoria.");
        if (t.length() > max) throw new IllegalArgumentException(campo + " admite maximo " + max + " caracteres.");
        return t;
    }

    protected static int anio(int v){
        int tope = LocalDate.now().getYear() + 1;
        if (v < 1800 || v > tope) throw new IllegalArgumentException("El año debe estar entre 1800 y " + tope + ".");
        return v;
    }
}
