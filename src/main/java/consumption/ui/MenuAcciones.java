package consumption.ui;

import consumption.model.ItemConsumption;
import consumption.model.Transicion;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuAcciones<T extends ItemConsumption> extends ContextMenu {

    public MenuAcciones(List<T> objetivo, Consumer<T> alEditar, Consumer<List<T>> alFavorito,
                        BiConsumer<T, Transicion> alCambiarEstado, Consumer<List<T>> alEliminar) {
        boolean uno = objetivo.size() == 1;
        T unico = uno ? objetivo.get(0) : null;

        MenuItem editar = decorar(new MenuItem("EDITAR"), "");
        editar.setDisable(!uno);
        editar.setOnAction(e -> alEditar.accept(unico));

        boolean todasFavoritas = objetivo.stream().allMatch(ItemConsumption::isFavorito);
        MenuItem favorito = decorar(new MenuItem(todasFavoritas ? "QUITAR FAVORITO" : "FAVORITO"), "★");
        favorito.setOnAction(e -> alFavorito.accept(objetivo));

        Menu estado = new Menu("CAMBIAR ESTADO");
        decorar(estado, "");
        if (uno) {
            for (Transicion transicion : Transicion.values()) {
                if (!transicion.permitidaDesde(unico.getEstado())) continue;
                MenuItem opcion = decorar(new MenuItem(transicion.getEtiqueta()), "");
                opcion.setOnAction(e -> alCambiarEstado.accept(unico, transicion));
                estado.getItems().add(opcion);
            }
        }
        estado.setDisable(estado.getItems().isEmpty());

        MenuItem eliminar = decorar(new MenuItem(ReglasTabla.textoEliminar(objetivo.size())), "");
        eliminar.getStyleClass().add("menu-peligro");
        eliminar.setOnAction(e -> alEliminar.accept(objetivo));

        getItems().addAll(editar, favorito, estado, new SeparatorMenuItem(), eliminar);
    }

    private static <M extends MenuItem> M decorar(M opcion, String icono) {
        Label propio = new Label(icono);
        propio.getStyleClass().add("menu-icono");
        Label flecha = new Label("▶");
        flecha.getStyleClass().add("menu-flecha");
        StackPane marca = new StackPane(propio, flecha);
        marca.setMinWidth(20);
        marca.setPrefWidth(20);
        opcion.setGraphic(marca);
        return opcion;
    }
}