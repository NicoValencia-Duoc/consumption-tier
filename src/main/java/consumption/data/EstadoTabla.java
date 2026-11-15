package consumption.data;

import java.util.List;

public class EstadoTabla {

    private List<String> columnas;
    private String ordenColumna;
    private boolean ordenAscendente;
    private String filtroEstado = "TODOS";
    private String filtroGenero = "TODOS";
    private boolean soloFavoritos;

    public List<String> getColumnas() { return columnas; }
    public String getOrdenColumna() { return ordenColumna; }
    public boolean isOrdenAscendente() { return ordenAscendente; }
    public String getFiltroEstado() { return filtroEstado == null ? "TODOS" : filtroEstado; }
    public String getFiltroGenero() { return filtroGenero == null ? "TODOS" : filtroGenero; }
    public boolean isSoloFavoritos() { return soloFavoritos; }

    public void setColumnas(List<String> v) { this.columnas = v; }
    public void setOrdenColumna(String v) { this.ordenColumna = v; }
    public void setOrdenAscendente(boolean v) { this.ordenAscendente = v; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
    public void setFiltroGenero(String v) { this.filtroGenero = v; }
    public void setSoloFavoritos(boolean v) { this.soloFavoritos = v; }
}