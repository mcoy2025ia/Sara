package entities;

import java.math.BigDecimal;

public record PresupuestoEvento(BigDecimal total, BigDecimal comprometido) {
    public BigDecimal disponible() { return total.subtract(comprometido); }
}
