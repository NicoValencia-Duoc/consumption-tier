package consumption.ui;

import consumption.model.Estado;
import consumption.model.ItemConsumption;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class PaginaTabla <T extends ItemConsumption> extends VBox{

    private static final int COMUNES = 5;
    private final TableView<T> tabla = new TableView<>();

    public PaginaTabla(String nombreTipo, List<T> items) {
        setSpacing(14);
        Label titulo = new Label(nombreTipo + " · " + items.size() + " REGISTROS");

        titulo.getStyleClass().add("subtitulo");

        tabla.setItems(FXCollections.observableArrayList(items));
        tabla.setPlaceholder(new Label("SIN REGISTROS TODAVIA"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.getColumns().add(columna("TITULO", ItemConsumption::getTitulo));

        TableColumn<T, Object> nota = columna("NOTA", ItemConsumption::getNota);
        nota.setSortType(TableColumn.SortType.DESCENDING);

        TableColumn<T, Object> estado = columna("ESTADO", ItemConsumption::getEstado);
        estado.setCellFactory(col -> new TableCell<>(){
            @Override protected void updateItem(Object valor, boolean vacio){
                super.updateItem(valor, vacio);
                for (Estado e : Estado.values())
                    getStyleClass().remove(e.getClaseCss());
                if (vacio || valor == null){
                    setText(null);
                    return;
                }
                Estado e = (Estado) valor;
                setText(e.getEtiqueta());

                getStyleClass().add(e.getClaseCss());
            }
        });

        tabla.getColumns().add(nota);
        tabla.getColumns().add(estado);

        tabla.getColumns().add(columna("AVANCE (%)", ItemConsumption::getPorcentaje));
        tabla.getColumns().add(columna("INICIO", i -> fecha(i.getFechaInicio(), "-")));
        tabla.getColumns().add(columna("TÉRMINO", i -> fecha(i.getFechaTermino(), "EN CURSO")));
        tabla.getSortOrder().add(nota);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getChildren().addAll(titulo, tabla);
    }

    public void agregarColumna(String titulo, Function<T, Object> extractor){

        tabla.getColumns().add(tabla.getColumns().size() - COMUNES, columna(titulo, extractor));
    }

    private TableColumn<T, Object> columna(String titulo, Function<T, Object> extractor){

        TableColumn<T, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(extractor.apply(d.getValue())));
        return c;
    }

    private static String fecha(LocalDate f, String siVacia){

        return (f == null) ? siVacia : f.format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));
    }
}

