package consumption.model;

public enum Estado {
    TERMINADO("TERMINADO", "estado-terminado"),
    EN_PROGRESO("EN PROGRESO", "estado-progreso"),
    ABANDONADO("ABANDONADO", "estado-abandonado"),
    PENDIENTE("PENDIENTE", "estado-pendiente");

    private final String etiqueta;
    private final String claseCss;

    Estado(String etiqueta, String claseCss){
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String getEtiqueta(){
        return etiqueta;
    }
    public String getClaseCss(){
        return claseCss;
    }
}
