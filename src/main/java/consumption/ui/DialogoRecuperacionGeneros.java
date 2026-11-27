package consumption.ui;

import consumption.data.RecuperacionGeneros;
import consumption.model.NombreGenero;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.util.*;

final class DialogoRecuperacionGeneros {
    private DialogoRecuperacionGeneros() {}

    static Optional<Set<Integer>> elegir(Window propietario, List<String> hojas, String titulo,
                                         RecuperacionGeneros.Plan plan) {
        if (plan.candidatos().stream().allMatch(RecuperacionGeneros.Candidato::disponible))
            return Optional.of(Set.of());
        Dialog<Set<Integer>> dialogo = new Dialog<>();
        if (propietario != null) dialogo.initOwner(propietario);
        dialogo.setTitle("THE MAW // RECUPERAR GENEROS");
        dialogo.setHeaderText("RECUPERAR GENEROS");
        VBox contenido = new VBox(8);
        Label nombre = new Label(titulo); nombre.setWrapText(true);
        contenido.getChildren().add(nombre);
        Map<Integer, CheckBox> casillas = new LinkedHashMap<>();
        for (var candidato : plan.candidatos()) {
            String texto = NombreGenero.mostrar(candidato.nombre());
            if (candidato.disponible()) {
                Label vigente = new Label(texto + " · SE CONSERVA"); vigente.setWrapText(true);
                contenido.getChildren().add(vigente);
            } else {
                CheckBox opcion = new CheckBox("RECUPERAR " + texto);
                opcion.setWrapText(true);
                casillas.put(candidato.indice(), opcion); contenido.getChildren().add(opcion);
            }
        }
        Label aviso = new Label("LOS TAGS NO MARCADOS NO SE RECUPERARAN. LOS MARCADOS PODRAN VOLVER A LA BIBLIOTECA.");
        aviso.setWrapText(true); contenido.getChildren().add(aviso);
        ScrollPane scroll = new ScrollPane(contenido); scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.setPrefViewportHeight(210);
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().getStylesheets().setAll(hojas);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Terminal.dialogo(dialogo, "RECUPERAR GENEROS", "boton-accion", "RECUPERAR");
        dialogo.setResultConverter(b -> {
            if (b != ButtonType.OK) return null;
            Set<Integer> seleccion = new HashSet<>();
            casillas.forEach((i, c) -> { if (c.isSelected()) seleccion.add(i); });
            return Set.copyOf(seleccion);
        });
        return dialogo.showAndWait();
    }
}
