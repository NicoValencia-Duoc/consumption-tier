package consumption.model;

import java.time.LocalDate;

public enum Transicion {

    EMPEZAR("EMPEZAR"),
    TERMINAR("TERMINAR"),
    ABANDONAR("ABANDONAR");

    private final String etiqueta;

    Transicion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean permitidaDesde(Estado estado) {
        return switch (this) {
            case EMPEZAR -> estado == Estado.PENDIENTE;
            case TERMINAR -> estado == Estado.PENDIENTE || estado == Estado.EN_PROGRESO;
            case ABANDONAR -> estado == Estado.EN_PROGRESO;
        };
    }

    public void aplicar(ItemConsumption item, LocalDate hoy) {
        Estado actual = item.getEstado();
        if (!permitidaDesde(actual)) {
            throw new IllegalArgumentException(
                    "No se puede " + etiqueta + " un registro que esta " + actual.getEtiqueta() + ".");
        }

        switch (this) {
            case EMPEZAR -> {
                if (item.getFechaTermino() != null && hoy.isAfter(item.getFechaTermino())) {
                    throw new IllegalArgumentException(
                            "La fecha de termino es anterior a hoy. Corrigela con EDITAR.");
                }
                item.setEstado(Estado.EN_PROGRESO);
                item.setFechaInicio(hoy);
            }
            case TERMINAR -> {
                comprobarInicio(item, hoy);
                item.setEstado(Estado.TERMINADO);
                item.setFechaTermino(hoy);
                item.setPorcentaje(Math.max(100, item.getPorcentaje()));
            }
            case ABANDONAR -> {
                comprobarInicio(item, hoy);
                item.setEstado(Estado.ABANDONADO);
                item.setFechaTermino(hoy);
            }
        }
        item.validarCoherencia();
    }

    private static void comprobarInicio(ItemConsumption item, LocalDate hoy) {
        if (item.getFechaInicio() != null && item.getFechaInicio().isAfter(hoy)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio es posterior a hoy. Corrigela con EDITAR.");
        }
    }
}