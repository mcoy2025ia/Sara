import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * CU-21: ejecuta con JUnit (y por lo tanto con mvn test) las verificaciones
 * de segmentación de clientes, que fueron escritas como programas con main.
 * Cada una lanza AssertionError si alguna comprobación falla.
 */
class MarketingCU21Test {

    @Test
    void repositorioLeeClientesYSolicitudesDelSeed() {
        assertDoesNotThrow(() -> PruebaMarketingRepositorio.main(new String[0]));
    }

    @Test
    void reglasDeSegmentacionAgrupanSoloSegmentosValidos() {
        assertDoesNotThrow(() -> PruebaMarketingSegmentacion.main(new String[0]));
    }

    @Test
    void administradorGeneraSegmentosYSeRechazanUsuariosSinPermiso() {
        assertDoesNotThrow(() -> PruebaMarketingCU21.main(new String[0]));
    }
}
