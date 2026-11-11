package consumption.ui;

import consumption.model.Estado;
import consumption.model.Pelicula;

import javafx.scene.control.TextField;
import java.util.List;

public class FormularioPelicula extends FormularioItem<Pelicula> {

    private final TextField director = new TextField();
    private final TextField genero = new TextField();
    private final TextField duracion = new TextField();
    private final TextField anio = new TextField();

    @Override protected List<String> nombresCriterios() { return Pelicula.CRITERIOS; }

    @Override protected String nombreTipo() { return "PELICULA"; }

    @Override protected void agregarCamposPropios() {
        agregarFila("DIRECTOR", director);
        agregarFila("GENERO", genero);
        agregarFila("DURACION (MIN)", duracion);
        agregarFila("AÑO", anio);
    }

    @Override protected void cargarPropios(Pelicula p) {
        director.setText(p.getDirector());
        genero.setText(p.getGenero());
        duracion.setText(String.valueOf(p.getDuracionMin()));
        anio.setText(String.valueOf(p.getAnio()));
    }

    @Override protected Pelicula crear(String titulo, double nota, Estado estado) {
        return new Pelicula(titulo, nota, estado, texto(director), texto(genero),
                entero(duracion, "La duración"), entero(anio, "El año"));
    }

    @Override protected void aplicarPropios(Pelicula p) {
        p.setDirector(texto(director));
        p.setGenero(texto(genero));
        p.setDuracionMin(entero(duracion, "La duración"));
        p.setAnio(entero(anio, "El año"));
    }
}
