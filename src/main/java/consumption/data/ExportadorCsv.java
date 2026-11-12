package consumption.data;

import consumption.model.Estado;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExportadorCsv {

    private static final String SEPARADOR = ";";

    public static <T> void exportar(TableView<T> tabla, Path destino) throws IOException {
        List<TableColumn<T, ?>> columnas = tabla.getColumns();
        StringBuilder csv = new StringBuilder("\uFEFF");

        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) csv.append(SEPARADOR);
            csv.append(campo(columnas.get(i).getText()));
        }
        csv.append("\r\n");

        for (T fila : tabla.getItems()) {
            for (int i = 0; i < columnas.size(); i++) {
                if (i > 0) csv.append(SEPARADOR);
                csv.append(campo(texto(columnas.get(i).getCellData(fila))));
            }
            csv.append("\r\n");
        }
        Files.writeString(destino, csv.toString(), StandardCharsets.UTF_8);
    }

    private static String texto(Object valor) {
        if (valor == null) return "";
        if (valor instanceof Estado e) return e.getEtiqueta();
        if (valor instanceof Double d) return String.valueOf(d).replace('.', ',');
        return valor.toString();
    }

    private static String campo(String texto) {
        String t = texto;
        boolean pareceFormula = !t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0 && !t.equals("-");
        if (pareceFormula) {
            t = "'" + t;
        }
        if (t.contains(SEPARADOR) || t.contains("\"") || t.contains("\n")) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }
}