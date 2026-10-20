package views;

import java.math.BigDecimal;
import java.util.regex.Pattern;

final class PrecioPropuestaParser {

    private static final Pattern FORMATO_PRECIO
            = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

    private PrecioPropuestaParser() {
    }

    static BigDecimal parsear(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new NumberFormatException("El precio esta vacio.");
        }

        String precio = entrada.trim();
        if (!FORMATO_PRECIO.matcher(precio).matches()) {
            throw new NumberFormatException(
                    "El precio no usa el formato decimal admitido.");
        }

        BigDecimal valor = new BigDecimal(precio);
        if (valor.scale() > 2
                && valor.stripTrailingZeros().scale() > 2) {
            throw new NumberFormatException(
                    "El precio no puede tener mas de dos decimales.");
        }
        return valor;
    }
}
