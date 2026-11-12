package consumption.ui;

import consumption.model.Estado;
import consumption.model.Serie;

import javafx.scene.control.TextField;
import java.util.List;

public class FormularioSerie extends FormularioItem<Serie> {

    private final TextField creador = new TextField();
    private final TextField genero = new TextField();
    private final TextField anio = new TextField();
    private final TextField temporadas = new TextField();
    private final TextField episodios = new TextField();
    private final TextField origen = new TextField();

    @Override protected List<String> nombresCriterios() { return Serie.CRITERIOS; }

    @Override protected String nombreTipo() { return "SERIE"; }

    @Override protected void agregarCamposPropios() {
        origen.setPromptText("OPCIONAL");
        agregarFila("CREADOR", creador);
        agregarFila("GENERO", genero);
        agregarFila("AÑO DE ESTRENO", anio);
        agregarFila("TEMPORADAS", temporadas);
        agregarFila("EPISODIOS TOTALES", episodios);
        agregarFila("ORIGEN", origen);
    }

    @Override protected void cargarPropios(Serie s) {
        creador.setText(s.getCreador());
        genero.setText(s.getGenero());
        anio.setText(String.valueOf(s.getAnio()));
        temporadas.setText(String.valueOf(s.getTemporadas()));
        episodios.setText(String.valueOf(s.getEpisodios()));
        origen.setText(s.getOrigen());
    }

    @Override protected Serie crear(String titulo, double nota, Estado estado) {
        return new Serie(titulo, nota, estado, texto(creador), texto(genero),
                entero(anio, "El año"), entero(temporadas, "Las temporadas"),
                entero(episodios, "Los episodios"), texto(origen));
    }

    @Override protected void aplicarPropios(Serie s) {
        s.setCreador(texto(creador));
        s.setGenero(texto(genero));
        s.setAnio(entero(anio, "El año"));
        s.setTemporadas(entero(temporadas, "Las temporadas"));
        s.setEpisodios(entero(episodios, "Los episodios"));
        s.setOrigen(texto(origen));
    }
}