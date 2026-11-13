package consumption.ui;

import consumption.model.ItemConsumption;
import consumption.model.Tier;
import javafx.scene.control.Label;

public class EtiquetaTier extends Label {

    public EtiquetaTier(Tier tier, String variante) {
        super(tier.getLetra());
        getStyleClass().addAll("tier", tier.getClaseCss());
        if (variante != null) getStyleClass().add(variante);
    }

    public static EtiquetaTier de(ItemConsumption item) {
        return de(item, null);
    }

    public static EtiquetaTier de(ItemConsumption item, String variante) {
        Tier tier = item.getTier();
        return (tier == null) ? null : new EtiquetaTier(tier, variante);
    }
}