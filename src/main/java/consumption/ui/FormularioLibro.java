package consumption.ui;

import consumption.model.Estado;
import consumption.model.Libro;

import javafx.scene.control.TextField;
import java.util.List;


public class FormularioLibro extends FormularioItem<Libro> {

    private final TextField autor = new TextField();
    private final TextField genero = new TextField();
    private final TextField paginas = new TextField();
    private final TextField anio = new TextField();

    @Override protected List<String> nombresCriterios() { return Libro.CRITERIOS; }

    @Override protected String nombreTipo() { return "LIBRO"; }

    @Override protected void agregarCamposPropios() {
        agregarFila("AUTOR", autor);
        agregarFila("GÉNERO", genero);
        agregarFila("PÁGINAS", paginas);
        agregarFila("AÑO", anio);
    }

    @Override protected void cargarPropios(Libro l) {
        autor.setText(l.getAutor());
        genero.setText(l.getGenero());
        paginas.setText(String.valueOf(l.getPaginas()));
        anio.setText(String.valueOf(l.getAnio()));
    }

    @Override protected Libro crear(String titulo, double nota, Estado estado) {
        return new Libro(titulo, nota, estado, texto(autor), texto(genero),
                entero(paginas, "Las páginas"), entero(anio, "El año"));
    }

    @Override protected void aplicarPropios(Libro l) {
        l.setAutor(texto(autor));
        l.setGenero(texto(genero));
        l.setPaginas(entero(paginas, "Las páginas"));
        l.setAnio(entero(anio, "El año"));
    }
}
