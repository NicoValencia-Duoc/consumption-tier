package consumption.ui;


import consumption.model.ItemConsumption;
import consumption.model.Estado;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import consumption.data.GestorImagenes;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PaginaDetalle extends VBox{

    public PaginaDetalle(ItemConsumption item, Runnable alGuardar, Runnable alVolver){

        setSpacing(12);
        Label titulo = new Label(item.getTitulo());
        titulo.getStyleClass().add("titulo-detalle");
        Label resumen = new Label(item.getTipo() + " · " + item.getEstado().getEtiqueta());
        resumen.getStyleClass().add("subtitulo");
        Label nota = new Label(String.format("NOTA FINAL %.1f", item.getNota()));
        nota.getStyleClass().add("nota");
        EtiquetaTier tier = EtiquetaTier.de(item, "tier-grande");
        HBox filaNota = new HBox(12, nota);
        filaNota.setAlignment(Pos.CENTER_LEFT);
        if (tier != null) filaNota.getChildren().add(tier);

        GridPane criterios = new GridPane();
        criterios.setHgap(24);
        criterios.setVgap(4);

        int fila = 0;
        for (String nombre : item.getNombresCriterios()){

            Double valor = item.getCriterios().get(nombre);
            Label numero = new Label(valor == null ? "N/A" : String.format("%.1f", valor));
            numero.getStyleClass().add("valor-criterio");
            criterios.add(new Label(nombre), 0, fila);
            criterios.add(numero, 1, fila);
            fila++;
        }

        Label tituloComentario = new Label("COMENTARIO");
        tituloComentario.getStyleClass().add("subtitulo");

        TextArea comentario = new TextArea(item.getComentario());
        comentario.setWrapText(true);
        comentario.setPromptText("Escribre lo que te parecio...");
        comentario.setPrefRowCount(6);
        comentario.setTextFormatter(new TextFormatter<String>(cambio -> cambio.getControlNewText().length() <= ItemConsumption.MAX_COMENTARIO ? cambio : null));

        Label contador = new Label();
        contador.getStyleClass().add("subtitulo");
        contador.textProperty().bind(comentario.textProperty().length().asString("%d/" + ItemConsumption.MAX_COMENTARIO));

        Label aviso = new Label();
        aviso.getStyleClass().add("valor-criterio");
        comentario.textProperty().addListener((obs, antes, ahora) -> aviso.setText(""));

        Button volver = new Button("← VOLVER");
        volver.getStyleClass().add("pestana");
        volver.setOnAction(e -> alVolver.run());

        Button guardar = new Button("GUARDAR COMENTARIO");
        guardar.getStyleClass().addAll("pestana", "boton-accion");
        guardar.setOnAction(e ->{
            item.setComentario(comentario.getText());
            alGuardar.run();
            aviso.setText("GUARDADO");
        });

        HBox botones = new HBox(10, volver, guardar, contador, aviso);
        botones.setAlignment(Pos.CENTER_LEFT);

        ImageView portada = new ImageView();
        Image imagen = GestorImagenes.imagenDe(item, 220, 300);
        if (imagen != null) portada.setImage(imagen);

        VBox datos = new VBox(8, titulo, resumen);
        if (item.getEstado() != Estado.PENDIENTE) {
            Label etiquetaAvance = new Label("AVANCE");
            etiquetaAvance.getStyleClass().add("subtitulo");
            datos.getChildren().add(new HBox(10, etiquetaAvance, new BarraAvance(item.getPorcentaje())));
        }
        datos.getChildren().addAll(filaNota, criterios);
        HBox cabeza = new HBox(24, portada, datos);
        getChildren().addAll(cabeza, tituloComentario, comentario, botones);
    }
}
