import config.TestSchema;
import org.junit.jupiter.api.Test;

class SchemaPersistenciaTest {
    @Test
    void inicializaMigraYConservaLosDatos() throws Exception {
        TestSchema.main(new String[0]);
    }

    @Test
    void almacenaTodosLosCamposDelEvento() throws Exception {
        PruebaEventoSchema.main(new String[0]);
    }
}
