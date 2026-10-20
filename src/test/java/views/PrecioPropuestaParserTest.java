package views;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class PrecioPropuestaParserTest {

    @Test
    void aceptaEnterosYDecimalesConFormatoDecimalExplicito() {
        assertEquals(
                new BigDecimal("1500000"),
                PrecioPropuestaParser.parsear(" 1500000 "));
        assertEquals(
                new BigDecimal("1.2300"),
                PrecioPropuestaParser.parsear("1.2300"));
        assertEquals(
                new BigDecimal("1500000.50"),
                PrecioPropuestaParser.parsear("1500000.50"));
        assertEquals(
                new BigDecimal("+1500000"),
                PrecioPropuestaParser.parsear("+1500000"));
    }

    @Test
    void rechazaEntradasVaciasNoNumericasYFormatosAmbiguos() {
        String[] invalidas = {
            null,
            "",
            "   ",
            "abc",
            "123abc",
            "@#$",
            "1.500.000",
            "1500000,50",
            "1e6",
            "NaN",
            "Infinity",
            "1500000.501",
            "1.234"
        };

        for (String entrada : invalidas) {
            assertThrows(
                    NumberFormatException.class,
                    () -> PrecioPropuestaParser.parsear(entrada),
                    () -> "Debe rechazar: " + entrada);
        }
    }
}
