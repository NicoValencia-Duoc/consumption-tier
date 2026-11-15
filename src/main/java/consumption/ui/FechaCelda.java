package consumption.ui;

import java.time.LocalDate;

public record FechaCelda(LocalDate fecha, String texto) {

    @Override
    public String toString() {
        return texto;
    }
}