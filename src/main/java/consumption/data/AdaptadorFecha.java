package consumption.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDate;

public class AdaptadorFecha extends TypeAdapter<LocalDate> {

    @Override
    public void write(JsonWriter out, LocalDate fecha) throws IOException {
        out.value(fecha.toString());
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
        return LocalDate.parse(in.nextString());
    }
}