package consumption.model;

import java.time.LocalDate;

public final class TransicionPersistida {
    private TransicionPersistida() {}

    public static void ejecutar(ItemConsumption item, Transicion transicion,
                                LocalDate fecha, Runnable persistir) {
        ItemConsumption.EstadoAnterior anterior = item.capturarEstado();
        try {
            transicion.aplicar(item, fecha);
            persistir.run();
        } catch (RuntimeException | Error fallo) {
            item.restaurarEstado(anterior);
            throw fallo;
        }
    }
}