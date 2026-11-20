package consumption.ui;


import consumption.model.ItemConsumption;
import consumption.model.Estado;
import consumption.model.Formato;
import consumption.model.Numeracion;
import consumption.model.ReglasInicio;
import consumption.model.Transicion;
import consumption.model.ReglasTermino;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import consumption.data.GestorImagenes;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.time.LocalDate;
import java.util.function.Consumer;

public class PaginaDetalle extends VBox{

    public PaginaDetalle(ItemConsumption item, Runnable alGuardar, Runnable alVolver,
                         Consumer<Transicion> alCambiarEstado, Runnable alEditar){

        setSpacing(12);
        Label titulo = new Label("> " + item.getTitulo());
        titulo.getStyleClass().add("titulo-detalle");
        Efectos.glitchAlPasar(titulo);
        Label resumen = new Label("// " + Nombres.singular(item.getTipo()) + " · " + item.getEstado().getEtiqueta());
        resumen.getStyleClass().add("subtitulo");
        Label nota = new Label("NOTA FINAL " + Formato.decimal(item.getNota()));
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
            Label numero = new Label(valor == null ? "N/A" : Formato.decimal(valor));
            numero.getStyleClass().add("valor-criterio");
            Label nombreCriterio = new Label(nombre);
            nombreCriterio.getStyleClass().add("nombre-criterio");
            criterios.add(nombreCriterio, 0, fila);
            criterios.add(new BarraLed(valor), 1, fila);
            criterios.add(numero, 2, fila);
            fila++;
        }

        String conQuien = item.getConQuien();
        Label tituloComentario = new Label(conQuien.isEmpty()
                ? "COMENTARIO.LOG" : "COMENTARIO.LOG · CON " + conQuien.toUpperCase());
        tituloComentario.getStyleClass().add("subtitulo");

        TextArea comentario = new TextArea(item.getComentario());
        comentario.setWrapText(true);
        comentario.setPromptText("Escribe lo que te parecio...");
        comentario.setPrefRowCount(6);
        comentario.setTextFormatter(new TextFormatter<String>(cambio -> cambio.getControlNewText().length() <= ItemConsumption.MAX_COMENTARIO ? cambio : null));

        Label contador = new Label();
        contador.getStyleClass().add("subtitulo");
        contador.textProperty().bind(comentario.textProperty().length().asString("%d/" + ItemConsumption.MAX_COMENTARIO));

        Label aviso = new Label();
        aviso.getStyleClass().add("valor-criterio");
        comentario.textProperty().addListener((obs, antes, ahora) -> aviso.setText(""));

        Button volver = Terminal.corchetes(new Button("← VOLVER"));
        volver.getStyleClass().add("pestana");
        volver.setOnAction(e -> alVolver.run());

        Button guardar = Terminal.corchetes(new Button("GUARDAR COMENTARIO"));
        guardar.getStyleClass().addAll("pestana", "boton-accion");
        guardar.setOnAction(e -> {
            String comentarioAnterior = item.getComentario();
            try {
                item.setComentario(comentario.getText());
                alGuardar.run();
                aviso.setText("GUARDADO");
            } catch (RuntimeException ex) {
                item.setComentario(comentarioAnterior);
                aviso.setText("NO SE PUDO GUARDAR");
            }
        });

        Button editar = Terminal.corchetes(new Button("EDITAR"));
        editar.getStyleClass().add("pestana");
        editar.setOnAction(e -> alEditar.run());
        Button copiar = Terminal.corchetes(new Button("COPIAR DETALLES"));
        copiar.getStyleClass().add("pestana");
        copiar.setOnAction(e -> {
            ClipboardContent portapapeles = new ClipboardContent();
            portapapeles.putString(textoParaCopiar(item));
            boolean correcto = Clipboard.getSystemClipboard().setContent(portapapeles);
            aviso.setText(correcto ? "DETALLES COPIADOS" : "NO SE PUDO COPIAR");
        });
        HBox botones = new HBox(10, volver, editar, guardar, copiar, contador, aviso);
        botones.setAlignment(Pos.CENTER_LEFT);

        ImageView portada = new ImageView();
        Image imagen = GestorImagenes.imagenDe(item, 220, 300);
        if (imagen != null) portada.setImage(imagen);

        VBox datos = new VBox(8, titulo, resumen);
        String textoDias = ReglasInicio.textoDias(item, LocalDate.now());
        if (textoDias != null) {
            Label dias = new Label(textoDias);
            dias.getStyleClass().add("dias-en-curso");
            datos.getChildren().add(dias);
        }
        String textoTermino = ReglasTermino.textoDias(item, LocalDate.now());
        if (textoTermino != null) {
            Label tiempoTerminado = new Label(textoTermino);
            tiempoTerminado.getStyleClass().add("subtitulo");
            datos.getChildren().add(tiempoTerminado);
        }
        int veces = item.getVecesConsumido();
        if (veces > 0) {
            Label consumido = new Label("CONSUMIDO " + veces + (veces == 1 ? " VEZ" : " VECES"));
            consumido.getStyleClass().add("subtitulo");
            datos.getChildren().add(consumido);
        }

        HBox estados = new HBox(8);
        for (Transicion transicion : Transicion.values()) {
            if (!transicion.permitidaDesde(item.getEstado())) continue;
            Button boton = Terminal.corchetes(new Button(transicion.getEtiqueta()));
            boton.getStyleClass().add("pestana");
            if (transicion == Transicion.TERMINAR) boton.getStyleClass().add("boton-accion");
            if (transicion == Transicion.ABANDONAR) boton.getStyleClass().add("boton-peligro");
            boton.setOnAction(e -> alCambiarEstado.accept(transicion));
            estados.getChildren().add(boton);
        }
        if (!estados.getChildren().isEmpty()) datos.getChildren().add(estados);

        boolean avanceFijo = item.getEstado() == Estado.TERMINADO && item.getPorcentajeMaximo() <= 100;
        if (item.getEstado() != Estado.PENDIENTE && !avanceFijo) {
            Label etiquetaAvance = new Label("AVANCE");
            etiquetaAvance.getStyleClass().add("subtitulo");

            DeslizadorAvance avance = new DeslizadorAvance(item.getPorcentajeMaximo() > 100);
            avance.setValor(item.getPorcentaje());

            Label avisoAvance = new Label();
            avisoAvance.getStyleClass().add("valor-criterio");
            avance.valorProperty().addListener((obs, antes, ahora) -> avisoAvance.setText(""));
            avance.setAlConfirmar(() -> {
                double porcentajeAnterior = item.getPorcentaje();
                try {
                    item.setPorcentaje(avance.getValor());
                    alGuardar.run();
                    avisoAvance.setText("AVANCE GUARDADO");
                } catch (IllegalArgumentException ex) {
                    item.setPorcentaje(porcentajeAnterior);
                    avance.setValor(porcentajeAnterior);
                    avisoAvance.setText(ex.getMessage());
                } catch (RuntimeException ex) {
                    item.setPorcentaje(porcentajeAnterior);
                    avance.setValor(porcentajeAnterior);
                    avisoAvance.setText("NO SE PUDO GUARDAR AVANCE");
                }
            });
            datos.getChildren().add(new HBox(10, etiquetaAvance, avance, avisoAvance));
        }
        if (avanceFijo) {
            Label etiquetaFija = new Label("AVANCE");
            etiquetaFija.getStyleClass().add("subtitulo");
            BarraAvance fija = new BarraAvance();
            fija.setPorcentaje(item.getPorcentaje());
            datos.getChildren().add(new HBox(10, etiquetaFija, fija));
        }
        datos.getChildren().addAll(filaNota, Terminal.panel("CRITERIOS", criterios));
        HBox cabeza = new HBox(24, portada, datos);
        getChildren().addAll(
                Terminal.panel("FICHA " + Numeracion.etiqueta(item.getNumero()).replace("#", ""), cabeza),
                Terminal.panel(tituloComentario.getText(), comentario),
                botones);
    }

    private static String textoParaCopiar(ItemConsumption item) {
        StringBuilder texto = new StringBuilder();
        texto.append("THE MAW · ").append(item.getTipo()).append('\n');
        texto.append("TITULO: ").append(item.getTitulo()).append('\n');
        texto.append("ESTADO: ").append(item.getEstado().getEtiqueta()).append('\n');
        texto.append("NOTA: ").append(Formato.decimal(item.getNota())).append('\n');
        if (item.getTier() != null) {
            texto.append("TIER: ").append(item.getTier().getLetra()).append('\n');
        }
        texto.append("AVANCE: ").append(Formato.decimal(item.getPorcentaje())).append("%\n");
        texto.append("VECES CONSUMIDO: ").append(item.getVecesConsumido()).append('\n');
        if (item.getFechaInicio() != null) {
            texto.append("INICIO: ").append(item.getFechaInicio()).append('\n');
        }
        if (item.getFechaTermino() != null) {
            texto.append("TERMINO: ").append(item.getFechaTermino()).append('\n');
        }
        if (item.getConQuien() != null && !item.getConQuien().isBlank()) {
            texto.append("CON QUIEN: ").append(item.getConQuien()).append('\n');
        }
        texto.append("\nCRITERIOS\n");
        for (String nombre : item.getNombresCriterios()) {
            Double valor = item.getCriterios().get(nombre);
            texto.append("- ").append(nombre).append(": ")
                    .append(valor == null ? "N/A" : Formato.decimal(valor)).append('\n');
        }
        if (item.getComentario() != null && !item.getComentario().isBlank()) {
            texto.append("\nCOMENTARIO\n").append(item.getComentario()).append('\n');
        }
        return texto.toString();
    }
}