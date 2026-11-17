package consumption.ui;

import consumption.model.ItemConsumption;
import consumption.model.Tier;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public class EtiquetaTier extends Label {

    public EtiquetaTier(Tier tier, String variante) {
        super(tier.getLetra());
        getStyleClass().addAll("tier", tier.getClaseCss());
        if (variante != null) getStyleClass().add(variante);
        setTooltip(new Tooltip(descripcionTier(tier)));
    }

    public static EtiquetaTier de(ItemConsumption item) {
        return de(item, null);
    }

    public static EtiquetaTier de(ItemConsumption item, String variante) {
        Tier tier = item.getTier();
        return (tier == null) ? null : new EtiquetaTier(tier, variante);
    }
    private static String descripcionTier(Tier tier) {
        return switch (tier) {
            case S -> "TIER S · 9,5 a 10";
            case A -> "TIER A · 8,5 a menos de 9,5";
            case B -> "TIER B · 7,5 a menos de 8,5";
            case C -> "TIER C · 6,0 a menos de 7,5";
            case D -> "TIER D · 4,0 a menos de 6,0";
            case F -> "TIER F · 0 a menos de 4,0";
        };
    }
}